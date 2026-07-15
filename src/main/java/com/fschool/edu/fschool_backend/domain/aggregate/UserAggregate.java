package com.fschool.edu.fschool_backend.domain.aggregate;

import com.fschool.edu.fschool_backend.domain.constants.RoleCodes;
import com.fschool.edu.fschool_backend.domain.enums.Gender;
import com.fschool.edu.fschool_backend.domain.enums.UserStatus;
import com.fschool.edu.fschool_backend.domain.exception.BusinessRuleViolationException;
import com.fschool.edu.fschool_backend.domain.exception.DomainValidationException;
import com.fschool.edu.fschool_backend.domain.valueobject.AuditInfo;
import com.fschool.edu.fschool_backend.domain.valueobject.EntityId;
import com.fschool.edu.fschool_backend.domain.valueobject.PersonName;
import com.fschool.edu.fschool_backend.domain.valueobject.PhoneNumber;
import com.fschool.edu.fschool_backend.domain.valueobject.StudentCode;
import java.time.LocalDate;
import java.time.Instant;
import lombok.Getter;

@Getter
public class UserAggregate extends AuditableAggregateRoot {

    private EntityId classId;
    private PhoneNumber phone;
    private String passwordHash;
    private StudentCode studentCode;
    private PersonName fullName;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String avatarUrl;
    private String address;
    private String guardianName;
    private PhoneNumber guardianPhone;
    private String role;
    private UserStatus status;
    private Instant lastLoginAt;

    public UserAggregate(
            EntityId id,
            EntityId classId,
            PhoneNumber phone,
            String passwordHash,
            StudentCode studentCode,
            PersonName fullName,
            LocalDate dateOfBirth,
            Gender gender,
            String avatarUrl,
            String address,
            String guardianName,
            PhoneNumber guardianPhone,
            String role,
            UserStatus status,
            Instant lastLoginAt,
            AuditInfo auditInfo) {
        super(id, auditInfo);
        this.classId = classId;
        this.phone = phone;
        changePasswordHash(passwordHash);
        this.studentCode = studentCode;
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.avatarUrl = normalizeOptional(avatarUrl, 2048, "Avatar URL");
        this.address = normalizeOptional(address, 255, "Address");
        this.guardianName = normalizeOptional(guardianName, 150, "Guardian name");
        this.guardianPhone = guardianPhone;
        this.role = role == null ? RoleCodes.STUDENT : role;
        this.status = status == null ? UserStatus.ACTIVE : status;
        this.lastLoginAt = lastLoginAt;
    }

    public boolean isStudent() {
        return RoleCodes.STUDENT.equals(role);
    }

    public boolean canLogin() {
        return status == UserStatus.ACTIVE;
    }

    public void changeClass(EntityId classId) {
        this.classId = classId;
        markUpdated();
    }

    public void updateProfile(PersonName fullName, LocalDate dateOfBirth, Gender gender, String address) {
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.address = normalizeOptional(address, 255, "Address");
        markUpdated();
    }

    public void changeGuardian(String guardianName, PhoneNumber guardianPhone) {
        this.guardianName = normalizeOptional(guardianName, 150, "Guardian name");
        this.guardianPhone = guardianPhone;
        markUpdated();
    }

    public void changeAvatarUrl(String avatarUrl) {
        this.avatarUrl = normalizeOptional(avatarUrl, 2048, "Avatar URL");
        markUpdated();
    }

    public void changePasswordHash(String passwordHash) {
        passwordHash = passwordHash == null ? "" : passwordHash.trim();
        if (passwordHash.isBlank() || passwordHash.length() > 255) {
            throw new DomainValidationException("Password hash is required and must not exceed 255 characters");
        }
        this.passwordHash = passwordHash;
        markUpdated();
    }

    public void recordLogin(Instant loginTime) {
        if (!canLogin()) {
            throw new BusinessRuleViolationException("Only active users can login");
        }
        lastLoginAt = loginTime == null ? Instant.now() : loginTime;
        markUpdated();
    }

    public void lock() {
        status = UserStatus.LOCKED;
        markUpdated();
    }

    public void activate() {
        status = UserStatus.ACTIVE;
        markUpdated();
    }

    public void deactivate() {
        status = UserStatus.INACTIVE;
        markUpdated();
    }

    private String normalizeOptional(String value, int maxLength, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        value = value.trim();
        if (value.length() > maxLength) {
            throw new DomainValidationException(fieldName + " must not exceed " + maxLength + " characters");
        }
        return value;
    }
}
