package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.time.LocalDate;

public record TeacherProfileResponse(
        String id,
        String fullName,
        String teacherCode,
        String employeeCode,
        String email,
        String phone,
        LocalDate dateOfBirth,
        String gender,
        String address,
        String departmentName,
        String campusName,
        String roleLabel,
        String avatarText) {
}
