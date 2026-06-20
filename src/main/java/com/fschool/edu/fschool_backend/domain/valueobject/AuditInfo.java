package com.fschool.edu.fschool_backend.domain.valueobject;

import com.fschool.edu.fschool_backend.domain.exception.DomainValidationException;
import java.time.Instant;

public record AuditInfo(Instant createdAt, Instant updatedAt) {

    public AuditInfo {
        if (createdAt == null) {
            throw new DomainValidationException("Created time is required");
        }
        if (updatedAt == null) {
            throw new DomainValidationException("Updated time is required");
        }
        if (updatedAt.isBefore(createdAt)) {
            throw new DomainValidationException("Updated time cannot be before created time");
        }
    }

    public static AuditInfo now() {
        Instant now = Instant.now();
        return new AuditInfo(now, now);
    }

    public AuditInfo touch() {
        return new AuditInfo(createdAt, Instant.now());
    }
}
