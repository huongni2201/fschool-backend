package com.fschool.edu.fschool_backend.domain.aggregate;

import com.fschool.edu.fschool_backend.domain.exception.DomainValidationException;
import com.fschool.edu.fschool_backend.domain.valueobject.AuditInfo;
import com.fschool.edu.fschool_backend.domain.valueobject.DateRange;
import com.fschool.edu.fschool_backend.domain.valueobject.EntityId;
import lombok.Getter;

@Getter
public class SemesterAggregate extends AuditableAggregateRoot {

    private final EntityId schoolYearId;
    private short semesterNo;
    private String name;
    private DateRange dateRange;
    private boolean current;

    public SemesterAggregate(
            EntityId id,
            EntityId schoolYearId,
            short semesterNo,
            String name,
            DateRange dateRange,
            boolean current,
            AuditInfo auditInfo) {
        super(id, auditInfo);
        if (schoolYearId == null) {
            throw new DomainValidationException("School year id is required");
        }
        this.schoolYearId = schoolYearId;
        changeSemesterNo(semesterNo);
        rename(name);
        this.dateRange = dateRange;
        this.current = current;
    }

    public void changeSemesterNo(short semesterNo) {
        if (semesterNo != 1 && semesterNo != 2) {
            throw new DomainValidationException("Semester number must be 1 or 2");
        }
        this.semesterNo = semesterNo;
        markUpdated();
    }

    public void rename(String name) {
        name = name == null ? "" : name.trim();
        if (name.isBlank() || name.length() > 50) {
            throw new DomainValidationException("Semester name is required and must not exceed 50 characters");
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
