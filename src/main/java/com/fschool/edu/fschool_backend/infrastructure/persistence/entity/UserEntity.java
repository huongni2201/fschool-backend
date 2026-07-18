package com.fschool.edu.fschool_backend.infrastructure.persistence.entity;

import com.fschool.edu.fschool_backend.domain.enums.Gender;
import com.fschool.edu.fschool_backend.domain.enums.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class UserEntity extends AuditableEntity {

    @Column(name = "class_id")
    private UUID classId;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Column(unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "student_code", unique = true, length = 20)
    private String studentCode;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(length = 255)
    private String address;

    @Column(name = "guardian_name", length = 150)
    private String guardianName;

    @Column(name = "guardian_phone", length = 20)
    private String guardianPhone;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role", referencedColumnName = "code", nullable = false)
    private RoleEntity role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;
}
