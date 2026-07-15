package com.fschool.edu.fschool_backend.presentation.dto.response;

import com.fschool.edu.fschool_backend.domain.enums.Gender;
import com.fschool.edu.fschool_backend.domain.enums.UserStatus;
import java.time.LocalDate;
import java.util.UUID;

public record RegisterResponse(
        UUID id,
        String phone,
        String studentCode,
        String fullName,
        UUID classId,
        LocalDate dateOfBirth,
        Gender gender,
        String avatarUrl,
        String address,
        String guardianName,
        String guardianPhone,
        String role,
        UserStatus status) {
}
