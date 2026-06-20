package com.fschool.edu.fschool_backend.domain.aggregate;

import com.fschool.edu.fschool_backend.domain.valueobject.AuditInfo;
import com.fschool.edu.fschool_backend.domain.valueobject.EntityId;
import lombok.Getter;

@Getter
public abstract class AuditableAggregateRoot extends AggregateRoot {

    private AuditInfo auditInfo;

    protected AuditableAggregateRoot(EntityId id, AuditInfo auditInfo) {
        super(id);
        this.auditInfo = auditInfo == null ? AuditInfo.now() : auditInfo;
    }

    protected void markUpdated() {
        auditInfo = auditInfo.touch();
    }
}
