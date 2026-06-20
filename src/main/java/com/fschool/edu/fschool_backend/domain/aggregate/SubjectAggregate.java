package com.fschool.edu.fschool_backend.domain.aggregate;

import com.fschool.edu.fschool_backend.domain.exception.DomainValidationException;
import com.fschool.edu.fschool_backend.domain.valueobject.AuditInfo;
import com.fschool.edu.fschool_backend.domain.valueobject.EntityId;
import lombok.Getter;

@Getter
public class SubjectAggregate extends AuditableAggregateRoot {

    private String code;
    private String name;
    private boolean scoreBased;

    public SubjectAggregate(EntityId id, String code, String name, boolean scoreBased, AuditInfo auditInfo) {
        super(id, auditInfo);
        changeCode(code);
        rename(name);
        this.scoreBased = scoreBased;
    }

    public void changeCode(String code) {
        code = code == null ? "" : code.trim().toUpperCase();
        if (code.isBlank() || code.length() > 20) {
            throw new DomainValidationException("Subject code is required and must not exceed 20 characters");
        }
        this.code = code;
        markUpdated();
    }

    public void rename(String name) {
        name = name == null ? "" : name.trim();
        if (name.isBlank() || name.length() > 100) {
            throw new DomainValidationException("Subject name is required and must not exceed 100 characters");
        }
        this.name = name;
        markUpdated();
    }

    public void markScoreBased() {
        scoreBased = true;
        markUpdated();
    }

    public void markNotScoreBased() {
        scoreBased = false;
        markUpdated();
    }
}
