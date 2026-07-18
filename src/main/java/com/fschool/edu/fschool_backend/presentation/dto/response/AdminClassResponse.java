package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.util.UUID;

public record AdminClassResponse(
        UUID id,
        String classCode,
        String name,
        int gradeLevel,
        long studentCount,
        UUID homeroomTeacherId,
        String homeroomTeacherCode,
        String homeroomTeacherName,
        String status) {
}
