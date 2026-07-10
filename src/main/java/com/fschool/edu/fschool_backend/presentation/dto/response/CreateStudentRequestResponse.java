package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.time.Instant;

public record CreateStudentRequestResponse(
        String id,
        String typeCode,
        String typeName,
        String title,
        String status,
        String statusLabel,
        Instant createdAt,
        Instant updatedAt) {
}
