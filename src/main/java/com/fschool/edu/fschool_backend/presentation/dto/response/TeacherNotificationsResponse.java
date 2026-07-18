package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.time.Instant;
import java.util.List;

public record TeacherNotificationsResponse(
        long unreadCount,
        List<NotificationItem> notifications) {

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
