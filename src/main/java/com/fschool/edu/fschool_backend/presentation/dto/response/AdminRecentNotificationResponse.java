package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AdminRecentNotificationResponse(
        UUID id,
        String title,
        String type,
        Instant createdAt) {
}
