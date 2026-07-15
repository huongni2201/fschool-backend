package com.fschool.edu.fschool_backend.application.service;

import com.fschool.edu.fschool_backend.application.command.LoginCommand;
import com.fschool.edu.fschool_backend.application.command.RegisterCommand;
import com.fschool.edu.fschool_backend.application.command.ResetPasswordCommand;
import com.fschool.edu.fschool_backend.application.command.SendOtpCommand;
import com.fschool.edu.fschool_backend.application.command.VerifyOtpCommand;
import com.fschool.edu.fschool_backend.domain.constants.RoleCodes;
import com.fschool.edu.fschool_backend.domain.enums.OtpPurpose;
import com.fschool.edu.fschool_backend.domain.enums.UserStatus;
import com.fschool.edu.fschool_backend.infrastructure.config.AuthProperties;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.ClassEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.OtpChallengeEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.RoleEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.UserEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.ClassJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.OtpChallengeJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.RoleJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.UserJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.security.CustomUserDetails;
import com.fschool.edu.fschool_backend.infrastructure.security.JwtService;
import com.fschool.edu.fschool_backend.infrastructure.security.ResetTokenService;
import com.fschool.edu.fschool_backend.presentation.dto.response.LoginResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.RegisterResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.SendOtpResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.VerifyOtpResponse;
import com.fschool.edu.fschool_backend.presentation.exception.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
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
    private final RoleJpaRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final ResetTokenService resetTokenService;
    private final AuthProperties authProperties;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginCommand command) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    command.username(),
                    command.password()));
        } catch (AuthenticationException exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Phone or password is invalid");
        }

        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        UserEntity user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Phone or password is invalid"));
        String roleCode = roleCode(user);
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(principal);
        String className = user.getClassId() == null
                ? null
                : classRepository.findById(user.getClassId()).map(ClassEntity::getName).orElse(null);
        String responseRole = responseRole(roleCode);
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.accessTokenTtlSeconds())
                .role(responseRole)
                .userId(user.getId())
                .studentCode(user.getStudentCode())
                .fullName(user.getFullName())
                .className(className)
                .build();
    }

    @Transactional
    public RegisterResponse register(RegisterCommand command) {
        String phone = normalizePhone(command.phone());
        String studentCode = normalizeOptionalText(command.studentCode());
        String guardianPhone = normalizePhone(command.guardianPhone());
        if (userRepository.existsByPhone(phone)) {
            throw new ApiException(HttpStatus.CONFLICT, "Phone already exists");
        }
        if (studentCode != null && userRepository.existsByStudentCode(studentCode)) {
            throw new ApiException(HttpStatus.CONFLICT, "Student code already exists");
        }
        if (studentCode != null && !hasText(guardianPhone)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Guardian phone is required for student account");
        }
        if (command.classId() != null && !classRepository.existsById(command.classId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Class was not found");
        }

        UserEntity user = new UserEntity();
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(command.password()));
        user.setStudentCode(studentCode);
        user.setFullName(normalizeText(command.fullName()));
        user.setClassId(command.classId());
        user.setDateOfBirth(command.dateOfBirth());
        user.setGender(command.gender());
        user.setAvatarUrl(normalizeText(command.avatarUrl()));
        user.setAddress(normalizeText(command.address()));
        user.setGuardianName(normalizeText(command.guardianName()));
        user.setGuardianPhone(guardianPhone);
        user.setRole(requireRole(registerRole(studentCode)));
        user.setStatus(UserStatus.ACTIVE);

        try {
            return toRegisterResponse(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            throw registerConflict(exception);
        }
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
        challenge.setOtpHash(hash(authProperties.getDevOtpCode()));
        challenge.setExpiresAt(Instant.now().plusSeconds(authProperties.getOtpTtlSeconds()));
        challenge.setAttemptCount(0);
        challenge.setMaxAttempts(5);
        OtpChallengeEntity saved = otpRepository.save(challenge);
        return new SendOtpResponse(saved.getId(), authProperties.getOtpTtlSeconds());
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

        user.setPasswordHash(passwordEncoder.encode(command.newPassword()));
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
        if (authProperties.getOtpResendCooldownSeconds() <= 0) {
            return;
        }
        Instant resendAllowedAfter = Instant.now().minusSeconds(authProperties.getOtpResendCooldownSeconds());
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
        String normalized = normalizeText(phone);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.replaceAll("[\\s.()-]", "");
        if (normalized.startsWith("+84")) {
            return "0" + normalized.substring(3);
        }
        if (normalized.startsWith("84") && normalized.length() == 11) {
            return "0" + normalized.substring(2);
        }
        return normalized;
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeOptionalText(String value) {
        String normalized = normalizeText(value);
        return hasText(normalized) ? normalized : null;
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
                responseRole(roleCode(user)),
                user.getStatus());
    }

    private RoleEntity requireRole(String roleCode) {
        return roleRepository.findById(roleCode)
                .orElseThrow(() -> new IllegalStateException("Role was not found: " + roleCode));
    }

    private String roleCode(UserEntity user) {
        RoleEntity role = user.getRole();
        if (role == null || !hasText(role.getCode())) {
            throw new IllegalStateException("User role is missing");
        }
        return role.getCode();
    }

    private String responseRole(String roleCode) {
        return roleCode == null ? null : roleCode.toLowerCase(Locale.ROOT);
    }

    private String registerRole(String studentCode) {
        return studentCode == null ? RoleCodes.PARENT : RoleCodes.STUDENT;
    }

    private ApiException registerConflict(DataIntegrityViolationException exception) {
        String message = exception.getMostSpecificCause().getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase(Locale.ROOT);
            if (lowerMessage.contains("phone")) {
                return new ApiException(HttpStatus.CONFLICT, "Phone already exists");
            }
            if (lowerMessage.contains("student_code")) {
                return new ApiException(HttpStatus.CONFLICT, "Student code already exists");
            }
        }
        return new ApiException(HttpStatus.CONFLICT, "User already exists");
    }
}
