package com.fschool.edu.fschool_backend.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StudentNotificationsResponse(
        long unreadCount,
        List<NotificationItem> notifications) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record NotificationItem(
            String id,
            String title,
            String message,
            String category,
            Instant createdAt,
            boolean isRead,
            String actionLabel,
            String deepLink) {
    }
}
