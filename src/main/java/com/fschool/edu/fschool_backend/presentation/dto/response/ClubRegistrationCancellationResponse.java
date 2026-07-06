package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.time.OffsetDateTime;

public record ClubRegistrationCancellationResponse(
        String clubId,
        String status,
        String statusLabel,
        OffsetDateTime updatedAt) {
}
