package com.fschool.edu.fschool_backend.domain.aggregate;

import com.fschool.edu.fschool_backend.domain.valueobject.AuditInfo;
import com.fschool.edu.fschool_backend.domain.valueobject.DateRange;
import com.fschool.edu.fschool_backend.domain.valueobject.EntityId;
import lombok.Getter;

@Getter
public class SchoolYearAggregate extends AuditableAggregateRoot {

    private String name;
    private DateRange dateRange;
    private boolean current;

    public SchoolYearAggregate(EntityId id, String name, DateRange dateRange, boolean current, AuditInfo auditInfo) {
        super(id, auditInfo);
        rename(name);
        this.dateRange = dateRange;
        this.current = current;
    }

    public static SchoolYearAggregate create(String name, DateRange dateRange) {
        return new SchoolYearAggregate(EntityId.newId(), name, dateRange, false, AuditInfo.now());
    }

    public void rename(String name) {
        name = name == null ? "" : name.trim();
        if (name.isBlank() || name.length() > 20) {
            throw new com.fschool.edu.fschool_backend.domain.exception.DomainValidationException(
                    "School year name is required and must not exceed 20 characters");
        }
        this.name = name;
        markUpdated();
    }

    public void changeDateRange(DateRange dateRange) {
        this.dateRange = dateRange;
        markUpdated();
    }

    public void markCurrent() {
        current = true;
        markUpdated();
    }

    public void markNotCurrent() {
        current = false;
        markUpdated();
    }
}
