package com.fschool.edu.fschool_backend.domain.aggregate;

import com.fschool.edu.fschool_backend.domain.exception.DomainValidationException;
import com.fschool.edu.fschool_backend.domain.valueobject.EntityId;
import java.time.Instant;
import lombok.Getter;

@Getter
public class NotificationAggregate extends AggregateRoot {

    private final EntityId userId;
    private String title;
    private String body;
    private String notificationType;
    private String deepLink;
    private boolean read;
    private Instant readAt;
    private final Instant createdAt;

    public NotificationAggregate(
            EntityId id,
            EntityId userId,
            String title,
            String body,
            String notificationType,
            String deepLink,
            boolean read,
            Instant readAt,
            Instant createdAt) {
        super(id);
        if (userId == null) {
            throw new DomainValidationException("Notification user id is required");
        }
        this.userId = userId;
        rename(title);
        changeBody(body);
        changeNotificationType(notificationType);
        this.deepLink = normalizeOptional(deepLink, 2048, "Deep link");
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
        if (read) {
            markRead(readAt);
        } else if (readAt != null) {
            throw new DomainValidationException("Unread notifications cannot have read time");
        }
    }

    public void rename(String title) {
        title = title == null ? "" : title.trim();
        if (title.isBlank() || title.length() > 255) {
            throw new DomainValidationException("Notification title is required and must not exceed 255 characters");
        }
        this.title = title;
    }

    public void changeBody(String body) {
        body = body == null ? "" : body.trim();
        if (body.isBlank()) {
            throw new DomainValidationException("Notification body is required");
        }
        this.body = body;
    }

    public void changeNotificationType(String notificationType) {
        notificationType = notificationType == null ? "" : notificationType.trim();
        if (notificationType.isBlank() || notificationType.length() > 50) {
            throw new DomainValidationException(
                    "Notification type is required and must not exceed 50 characters");
        }
        this.notificationType = notificationType;
    }

    public void markRead(Instant readAt) {
        this.read = true;
        this.readAt = readAt == null ? Instant.now() : readAt;
    }

    public void markUnread() {
        this.read = false;
        this.readAt = null;
    }

    private String normalizeOptional(String value, int maxLength, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        value = value.trim();
        if (value.length() > maxLength) {
            throw new DomainValidationException(fieldName + " must not exceed " + maxLength + " characters");
        }
        return value;
    }
}
