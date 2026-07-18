package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AdminRecentImportResponse(
        UUID id,
        String fileName,
        String importType,
        String status,
        long successRows,
        long failedRows,
        Instant createdAt,
        String createdBy) {
}
