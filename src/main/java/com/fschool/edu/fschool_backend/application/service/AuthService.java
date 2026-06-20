package com.fschool.edu.fschool_backend.application.service;

import com.fschool.edu.fschool_backend.presentation.dto.response.LoginResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.LoginUserResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.SendOtpResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.VerifyOtpResponse;
import com.fschool.edu.fschool_backend.application.command.LoginCommand;
import com.fschool.edu.fschool_backend.application.command.ResetPasswordCommand;
import com.fschool.edu.fschool_backend.application.command.SendOtpCommand;
import com.fschool.edu.fschool_backend.application.command.VerifyOtpCommand;
import com.fschool.edu.fschool_backend.presentation.exception.ApiException;
import com.fschool.edu.fschool_backend.infrastructure.security.PasswordService;
import com.fschool.edu.fschool_backend.infrastructure.security.ResetTokenService;
import com.fschool.edu.fschool_backend.infrastructure.security.TokenService;
import com.fschool.edu.fschool_backend.domain.enums.OtpPurpose;
import com.fschool.edu.fschool_backend.domain.enums.UserStatus;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.ClassEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.OtpChallengeEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.UserEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.ClassJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.OtpChallengeJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.UserJpaRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserJpaRepository userRepository;
    private final ClassJpaRepository classRepository;
    private final OtpChallengeJpaRepository otpRepository;
    private final PasswordService passwordService;
    private final TokenService tokenService;
    private final ResetTokenService resetTokenService;
    private final String devOtpCode;
    private final long otpTtlSeconds;

    public AuthService(
            UserJpaRepository userRepository,
            ClassJpaRepository classRepository,
            OtpChallengeJpaRepository otpRepository,
            PasswordService passwordService,
            TokenService tokenService,
            ResetTokenService resetTokenService,
            @Value("${app.auth.dev-otp-code:123456}") String devOtpCode,
            @Value("${app.auth.otp-ttl-seconds:300}") long otpTtlSeconds) {
        this.userRepository = userRepository;
        this.classRepository = classRepository;
        this.otpRepository = otpRepository;
        this.passwordService = passwordService;
        this.tokenService = tokenService;
        this.resetTokenService = resetTokenService;
        this.devOtpCode = devOtpCode;
        this.otpTtlSeconds = otpTtlSeconds;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginCommand command) {
        UserEntity user = userRepository.findByPhone(command.phone())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Phone or password is invalid"));
        if (user.getStatus() != UserStatus.ACTIVE || !passwordService.matches(command.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Phone or password is invalid");
        }
        String accessToken = tokenService.createAccessToken(user.getId(), user.getRole());
        String className = user.getClassId() == null
                ? null
                : classRepository.findById(user.getClassId()).map(ClassEntity::getName).orElse(null);
        LoginUserResponse loginUser = new LoginUserResponse(
                user.getId(),
                user.getStudentCode(),
                user.getFullName(),
                user.getRole(),
                className);
        return new LoginResponse(accessToken, "Bearer", tokenService.accessTokenTtlSeconds(), loginUser);
    }

    @Transactional
    public SendOtpResponse sendForgotPasswordOtp(SendOtpCommand command) {
        userRepository.findByPhone(command.phone())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User phone was not found"));

        OtpChallengeEntity challenge = new OtpChallengeEntity();
        challenge.setPhone(command.phone());
        challenge.setPurpose(OtpPurpose.FORGOT_PASSWORD);
        challenge.setOtpHash(hash(devOtpCode));
        challenge.setExpiresAt(Instant.now().plusSeconds(otpTtlSeconds));
        challenge.setAttemptCount(0);
        challenge.setMaxAttempts(5);
        OtpChallengeEntity saved = otpRepository.save(challenge);
        return new SendOtpResponse(saved.getId(), otpTtlSeconds);
    }

    @Transactional
    public VerifyOtpResponse verifyForgotPasswordOtp(VerifyOtpCommand command) {
        OtpChallengeEntity challenge = otpRepository.findById(command.challengeId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "OTP challenge was not found"));
        Instant now = Instant.now();
        if (challenge.getUsedAt() != null || !now.isBefore(challenge.getExpiresAt())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OTP challenge is invalid or expired");
        }
        if (challenge.getAttemptCount() >= challenge.getMaxAttempts()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OTP challenge has no attempts left");
        }
        if (!challenge.getOtpHash().equals(hash(command.otp()))) {
            challenge.setAttemptCount(challenge.getAttemptCount() + 1);
            otpRepository.save(challenge);
            throw new ApiException(HttpStatus.BAD_REQUEST, "OTP is invalid");
        }
        challenge.setVerifiedAt(now);
        otpRepository.save(challenge);
        UserEntity user = userRepository.findByPhone(challenge.getPhone())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User phone was not found"));
        return new VerifyOtpResponse(resetTokenService.create(user.getId()));
    }

    @Transactional
    public void resetPassword(ResetPasswordCommand command) {
        UUID userId = resetTokenService.consume(command.resetToken());
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User was not found"));
        user.setPasswordHash(passwordService.encode(command.newPassword()));
        userRepository.save(user);

        otpRepository.findByPhoneAndPurposeOrderByExpiresAtDesc(user.getPhone(), OtpPurpose.FORGOT_PASSWORD).stream()
                .filter(challenge -> challenge.getVerifiedAt() != null && challenge.getUsedAt() == null)
                .findFirst()
                .ifPresent(challenge -> {
                    challenge.setUsedAt(Instant.now());
                    otpRepository.save(challenge);
                });
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot hash value", exception);
        }
    }
}
