package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.time.OffsetDateTime;

public record ClubRegistrationResponse(
        String clubId,
        String registrationId,
        String status,
        String statusLabel,
        OffsetDateTime registeredAt) {
}
