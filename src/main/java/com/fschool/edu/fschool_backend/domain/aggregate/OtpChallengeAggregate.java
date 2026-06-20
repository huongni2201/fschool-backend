package com.fschool.edu.fschool_backend.domain.aggregate;

import com.fschool.edu.fschool_backend.domain.enums.OtpPurpose;
import com.fschool.edu.fschool_backend.domain.exception.BusinessRuleViolationException;
import com.fschool.edu.fschool_backend.domain.exception.DomainValidationException;
import com.fschool.edu.fschool_backend.domain.valueobject.EntityId;
import com.fschool.edu.fschool_backend.domain.valueobject.PhoneNumber;
import java.time.Instant;
import lombok.Getter;

@Getter
public class OtpChallengeAggregate extends AggregateRoot {

    private final PhoneNumber phone;
    private final String otpHash;
    private final OtpPurpose purpose;
    private final Instant expiresAt;
    private int attemptCount;
    private final int maxAttempts;
    private Instant verifiedAt;
    private Instant usedAt;
    private final Instant createdAt;

    public OtpChallengeAggregate(
            EntityId id,
            PhoneNumber phone,
            String otpHash,
            OtpPurpose purpose,
            Instant expiresAt,
            int attemptCount,
            int maxAttempts,
            Instant verifiedAt,
            Instant usedAt,
            Instant createdAt) {
        super(id);
        if (phone == null) {
            throw new DomainValidationException("OTP phone is required");
        }
        otpHash = otpHash == null ? "" : otpHash.trim();
        if (otpHash.isBlank() || otpHash.length() > 255) {
            throw new DomainValidationException("OTP hash is required and must not exceed 255 characters");
        }
        if (maxAttempts <= 0) {
            throw new DomainValidationException("Max attempts must be greater than zero");
        }
        if (attemptCount < 0) {
            throw new DomainValidationException("Attempt count cannot be negative");
        }
        Instant normalizedCreatedAt = createdAt == null ? Instant.now() : createdAt;
        if (expiresAt == null || !expiresAt.isAfter(normalizedCreatedAt)) {
            throw new DomainValidationException("OTP expiration must be after created time");
        }
        this.phone = phone;
        this.otpHash = otpHash;
        this.purpose = purpose == null ? OtpPurpose.FORGOT_PASSWORD : purpose;
        this.expiresAt = expiresAt;
        this.attemptCount = attemptCount;
        this.maxAttempts = maxAttempts;
        this.verifiedAt = verifiedAt;
        this.usedAt = usedAt;
        this.createdAt = normalizedCreatedAt;
    }

    public boolean isExpired(Instant now) {
        now = now == null ? Instant.now() : now;
        return !now.isBefore(expiresAt);
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public void registerFailedAttempt(Instant now) {
        ensureUsable(now);
        if (attemptCount >= maxAttempts) {
            throw new BusinessRuleViolationException("OTP challenge has no attempts left");
        }
        attemptCount++;
    }

    public void verify(Instant now) {
        ensureUsable(now);
        if (attemptCount >= maxAttempts) {
            throw new BusinessRuleViolationException("OTP challenge has no attempts left");
        }
        verifiedAt = now == null ? Instant.now() : now;
    }

    public void markUsed(Instant now) {
        ensureUsable(now);
        if (!isVerified()) {
            throw new BusinessRuleViolationException("OTP challenge must be verified before use");
        }
        usedAt = now == null ? Instant.now() : now;
    }

    private void ensureUsable(Instant now) {
        if (isExpired(now)) {
            throw new BusinessRuleViolationException("OTP challenge has expired");
        }
        if (isUsed()) {
            throw new BusinessRuleViolationException("OTP challenge has already been used");
        }
    }
}
