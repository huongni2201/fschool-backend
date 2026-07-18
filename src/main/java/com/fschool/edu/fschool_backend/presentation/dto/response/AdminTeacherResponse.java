package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.util.UUID;

public record AdminTeacherResponse(
        UUID id,
        String teacherCode,
        String fullName,
        String departmentId,
        String departmentName,
        String email,
        String phoneNumber,
        String status) {
}
