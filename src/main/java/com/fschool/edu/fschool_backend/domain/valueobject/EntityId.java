package com.fschool.edu.fschool_backend.domain.valueobject;

import com.fschool.edu.fschool_backend.domain.exception.DomainValidationException;
import java.util.UUID;

public record EntityId(UUID value) {

    public EntityId {
        if (value == null) {
            throw new DomainValidationException("Entity id is required");
        }
    }

    public static EntityId of(UUID value) {
        return new EntityId(value);
    }

    public static EntityId newId() {
        return new EntityId(UUID.randomUUID());
    }
}
