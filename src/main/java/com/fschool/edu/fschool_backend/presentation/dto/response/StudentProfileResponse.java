package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.time.LocalDate;

public record StudentProfileResponse(
        String studentId,
        String studentCode,
        String fullName,
        String displayName,
        String avatarUrl,
        String avatarText,
        String gender,
        LocalDate dateOfBirth,
        String email,
        String phone,
        String className,
        String campusName,
        String schoolYear) {
}
