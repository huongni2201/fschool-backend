package com.fschool.edu.fschool_backend.application.service;

import com.fschool.edu.fschool_backend.application.command.LoginCommand;
import com.fschool.edu.fschool_backend.application.command.RegisterCommand;
import com.fschool.edu.fschool_backend.application.command.ResetPasswordCommand;
import com.fschool.edu.fschool_backend.application.command.SendOtpCommand;
import com.fschool.edu.fschool_backend.application.command.VerifyOtpCommand;
import com.fschool.edu.fschool_backend.domain.enums.OtpPurpose;
import com.fschool.edu.fschool_backend.domain.enums.UserRole;
import com.fschool.edu.fschool_backend.domain.enums.UserStatus;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.ClassEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.OtpChallengeEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.UserEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.ClassJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.OtpChallengeJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.UserJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.security.PasswordService;
import com.fschool.edu.fschool_backend.infrastructure.security.ResetTokenService;
import com.fschool.edu.fschool_backend.infrastructure.security.TokenService;
import com.fschool.edu.fschool_backend.presentation.dto.response.LoginResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.LoginUserResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.RegisterResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.SendOtpResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.VerifyOtpResponse;
import com.fschool.edu.fschool_backend.presentation.exception.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Pattern OTP_PATTERN = Pattern.compile("\\d{4,8}");
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final String PHONE_NOT_FOUND_MESSAGE =
            "S\u1ED1 \u0111i\u1EC7n tho\u1EA1i kh\u00F4ng t\u1ED3n t\u1EA1i";
    private static final String OTP_INVALID_OR_EXPIRED_MESSAGE =
            "M\u00E3 OTP kh\u00F4ng \u0111\u00FAng ho\u1EB7c \u0111\u00E3 h\u1EBFt h\u1EA1n";
    private static final String OTP_TOO_MANY_REQUESTS_MESSAGE =
            "G\u1EEDi OTP qu\u00E1 nhi\u1EC1u l\u1EA7n";
    private static final String RESET_PASSWORD_FAILED_MESSAGE =
            "Kh\u00F4ng th\u1EC3 \u0111\u1EB7t l\u1EA1i m\u1EADt kh\u1EA9u";

    private final UserJpaRepository userRepository;
    private final ClassJpaRepository classRepository;
    private final OtpChallengeJpaRepository otpRepository;
    private final PasswordService passwordService;
    private final TokenService tokenService;
    private final ResetTokenService resetTokenService;
    private final String devOtpCode;
    private final long otpTtlSeconds;
    private final long otpResendCooldownSeconds;

    public AuthService(
            UserJpaRepository userRepository,
            ClassJpaRepository classRepository,
            OtpChallengeJpaRepository otpRepository,
            PasswordService passwordService,
            TokenService tokenService,
            ResetTokenService resetTokenService,
            @Value("${app.auth.dev-otp-code:123456}") String devOtpCode,
            @Value("${app.auth.otp-ttl-seconds:300}") long otpTtlSeconds,
            @Value("${app.auth.otp-resend-cooldown-seconds:60}") long otpResendCooldownSeconds) {
        this.userRepository = userRepository;
        this.classRepository = classRepository;
        this.otpRepository = otpRepository;
        this.passwordService = passwordService;
        this.tokenService = tokenService;
        this.resetTokenService = resetTokenService;
        this.devOtpCode = devOtpCode;
        this.otpTtlSeconds = otpTtlSeconds;
        this.otpResendCooldownSeconds = otpResendCooldownSeconds;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginCommand command) {
        UserEntity user = userRepository.findByPhone(command.username())
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
    public RegisterResponse register(RegisterCommand command) {
        userRepository.findByPhone(command.phone())
                .ifPresent(user -> {
                    throw new ApiException(HttpStatus.CONFLICT, "Phone already exists");
                });
        userRepository.findByStudentCode(command.studentCode())
                .ifPresent(user -> {
                    throw new ApiException(HttpStatus.CONFLICT, "Student code already exists");
                });
        if (command.classId() != null && !classRepository.existsById(command.classId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Class was not found");
        }

        UserEntity user = new UserEntity();
        user.setPhone(command.phone());
        user.setPasswordHash(passwordService.encode(command.password()));
        user.setStudentCode(command.studentCode());
        user.setFullName(command.fullName());
        user.setClassId(command.classId());
        user.setDateOfBirth(command.dateOfBirth());
        user.setGender(command.gender());
        user.setAvatarUrl(command.avatarUrl());
        user.setAddress(command.address());
        user.setGuardianName(command.guardianName());
        user.setGuardianPhone(command.guardianPhone());
        user.setRole(UserRole.STUDENT);
        user.setStatus(UserStatus.ACTIVE);

        return toRegisterResponse(userRepository.save(user));
    }

    @Transactional
    public SendOtpResponse sendForgotPasswordOtp(SendOtpCommand command) {
        String phone = normalizePhone(command == null ? null : command.phone());
        if (!hasText(phone)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, PHONE_NOT_FOUND_MESSAGE);
        }
        requireUserByPhone(phone, HttpStatus.NOT_FOUND, PHONE_NOT_FOUND_MESSAGE);
        enforceOtpSendCooldown(phone);

        OtpChallengeEntity challenge = new OtpChallengeEntity();
        challenge.setPhone(phone);
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
        String phone = normalizePhone(command == null ? null : command.phoneNumber());
        if (hasText(phone)) {
            requireUserByPhone(phone, HttpStatus.NOT_FOUND, PHONE_NOT_FOUND_MESSAGE);
        }
        OtpChallengeEntity challenge = resolveForgotPasswordChallenge(
                command == null ? null : command.challengeId(),
                phone,
                OTP_INVALID_OR_EXPIRED_MESSAGE);
        validateForgotPasswordOtp(
                challenge,
                command == null ? null : command.otp(),
                Instant.now(),
                OTP_INVALID_OR_EXPIRED_MESSAGE,
                true,
                false);
        UserEntity user = userRepository.findByPhone(challenge.getPhone())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, PHONE_NOT_FOUND_MESSAGE));
        return new VerifyOtpResponse(resetTokenService.create(user.getId()));
    }

    @Transactional
    public void resetPassword(ResetPasswordCommand command) {
        String newPassword = command == null ? null : command.newPassword();
        if (!hasText(newPassword) || newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw resetPasswordFailed(HttpStatus.BAD_REQUEST);
        }

        String phone = normalizePhone(command.phoneNumber());
        String resetToken = normalizeText(command.resetToken());
        String otp = command.otp();
        Instant now = Instant.now();

        UserEntity user;
        if (hasText(resetToken)) {
            user = consumeResetToken(resetToken);
            if (hasText(phone) && !phone.equals(user.getPhone())) {
                throw resetPasswordFailed(HttpStatus.BAD_REQUEST);
            }
            if (hasText(otp)) {
                OtpChallengeEntity challenge = resolveForgotPasswordChallenge(
                        null,
                        user.getPhone(),
                        RESET_PASSWORD_FAILED_MESSAGE);
                validateForgotPasswordOtp(challenge, otp, now, RESET_PASSWORD_FAILED_MESSAGE, true, true);
            } else {
                markLatestVerifiedOtpUsed(user.getPhone(), now);
            }
        } else {
            if (!hasText(phone)) {
                throw resetPasswordFailed(HttpStatus.BAD_REQUEST);
            }
            user = userRepository.findByPhone(phone)
                    .orElseThrow(() -> resetPasswordFailed(HttpStatus.NOT_FOUND));
            OtpChallengeEntity challenge = resolveForgotPasswordChallenge(
                    null,
                    phone,
                    RESET_PASSWORD_FAILED_MESSAGE);
            validateForgotPasswordOtp(challenge, otp, now, RESET_PASSWORD_FAILED_MESSAGE, true, true);
        }

        user.setPasswordHash(passwordService.encode(command.newPassword()));
        userRepository.save(user);
    }

    private UserEntity requireUserByPhone(String phone, HttpStatus status, String message) {
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new ApiException(status, message));
    }

    private OtpChallengeEntity resolveForgotPasswordChallenge(UUID challengeId, String phone, String message) {
        if (challengeId != null) {
            OtpChallengeEntity challenge = otpRepository.findById(challengeId)
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, message));
            if (challenge.getPurpose() != OtpPurpose.FORGOT_PASSWORD
                    || (hasText(phone) && !phone.equals(challenge.getPhone()))) {
                throw new ApiException(HttpStatus.BAD_REQUEST, message);
            }
            return challenge;
        }
        if (!hasText(phone)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, message);
        }
        return latestForgotPasswordChallenge(phone)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, message));
    }

    private Optional<OtpChallengeEntity> latestForgotPasswordChallenge(String phone) {
        return otpRepository.findByPhoneAndPurposeOrderByExpiresAtDesc(phone, OtpPurpose.FORGOT_PASSWORD).stream()
                .findFirst();
    }

    private void enforceOtpSendCooldown(String phone) {
        if (otpResendCooldownSeconds <= 0) {
            return;
        }
        Instant resendAllowedAfter = Instant.now().minusSeconds(otpResendCooldownSeconds);
        latestForgotPasswordChallenge(phone)
                .map(OtpChallengeEntity::getCreatedAt)
                .filter(createdAt -> createdAt != null && createdAt.isAfter(resendAllowedAfter))
                .ifPresent(createdAt -> {
                    throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, OTP_TOO_MANY_REQUESTS_MESSAGE);
                });
    }

    private void validateForgotPasswordOtp(
            OtpChallengeEntity challenge,
            String otp,
            Instant now,
            String message,
            boolean markVerified,
            boolean markUsed) {
        if (!hasValidOtpFormat(otp) || challenge.getUsedAt() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, message);
        }
        if (!now.isBefore(challenge.getExpiresAt())) {
            throw new ApiException(HttpStatus.GONE, message);
        }
        if (challenge.getAttemptCount() >= challenge.getMaxAttempts()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, message);
        }
        if (!challenge.getOtpHash().equals(hash(otp))) {
            challenge.setAttemptCount(challenge.getAttemptCount() + 1);
            otpRepository.save(challenge);
            throw new ApiException(HttpStatus.BAD_REQUEST, message);
        }
        if (markVerified && challenge.getVerifiedAt() == null) {
            challenge.setVerifiedAt(now);
        }
        if (markUsed) {
            challenge.setUsedAt(now);
        }
        otpRepository.save(challenge);
    }

    private UserEntity consumeResetToken(String resetToken) {
        UUID userId;
        try {
            userId = resetTokenService.consume(resetToken);
        } catch (ApiException exception) {
            throw resetPasswordFailed(HttpStatus.BAD_REQUEST);
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> resetPasswordFailed(HttpStatus.NOT_FOUND));
    }

    private void markLatestVerifiedOtpUsed(String phone, Instant now) {
        latestForgotPasswordChallenge(phone)
                .filter(challenge -> challenge.getVerifiedAt() != null && challenge.getUsedAt() == null)
                .ifPresent(challenge -> {
                    challenge.setUsedAt(now);
                    otpRepository.save(challenge);
                });
    }

    private ApiException resetPasswordFailed(HttpStatus status) {
        return new ApiException(status, RESET_PASSWORD_FAILED_MESSAGE);
    }

    private boolean hasValidOtpFormat(String otp) {
        return hasText(otp) && OTP_PATTERN.matcher(otp).matches();
    }

    private String normalizePhone(String phone) {
        return normalizeText(phone);
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot hash value", exception);
        }
    }

    private RegisterResponse toRegisterResponse(UserEntity user) {
        return new RegisterResponse(
                user.getId(),
                user.getPhone(),
                user.getStudentCode(),
                user.getFullName(),
                user.getClassId(),
                user.getDateOfBirth(),
                user.getGender(),
                user.getAvatarUrl(),
                user.getAddress(),
                user.getGuardianName(),
                user.getGuardianPhone(),
                user.getRole(),
                user.getStatus());
    }
}
