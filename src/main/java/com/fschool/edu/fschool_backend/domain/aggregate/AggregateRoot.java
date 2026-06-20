package com.fschool.edu.fschool_backend.domain.aggregate;

import com.fschool.edu.fschool_backend.domain.valueobject.EntityId;
import lombok.Getter;

@Getter
public abstract class AggregateRoot {

    private final EntityId id;

    protected AggregateRoot(EntityId id) {
        this.id = id;
    }
}
