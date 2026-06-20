package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String title,
        String body,
        String notificationType,
        String deepLink,
        Boolean isRead,
        Instant readAt,
        Instant createdAt) {
}
