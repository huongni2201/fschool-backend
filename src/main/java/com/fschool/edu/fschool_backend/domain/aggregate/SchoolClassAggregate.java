package com.fschool.edu.fschool_backend.domain.aggregate;

import com.fschool.edu.fschool_backend.domain.exception.DomainValidationException;
import com.fschool.edu.fschool_backend.domain.valueobject.AuditInfo;
import com.fschool.edu.fschool_backend.domain.valueobject.EntityId;
import com.fschool.edu.fschool_backend.domain.valueobject.GradeNumber;
import lombok.Getter;

@Getter
public class SchoolClassAggregate extends AuditableAggregateRoot {

    private final EntityId schoolYearId;
    private String name;
    private GradeNumber gradeNumber;
    private String homeroomTeacherName;
    private String roomName;

    public SchoolClassAggregate(
            EntityId id,
            EntityId schoolYearId,
            String name,
            GradeNumber gradeNumber,
            String homeroomTeacherName,
            String roomName,
            AuditInfo auditInfo) {
        super(id, auditInfo);
        if (schoolYearId == null) {
            throw new DomainValidationException("School year id is required");
        }
        this.schoolYearId = schoolYearId;
        rename(name);
        this.gradeNumber = gradeNumber;
        changeHomeroomTeacherName(homeroomTeacherName);
        changeRoomName(roomName);
    }

    public void rename(String name) {
        name = name == null ? "" : name.trim().toUpperCase();
        if (name.isBlank() || name.length() > 20) {
            throw new DomainValidationException("Class name is required and must not exceed 20 characters");
        }
        this.name = name;
        markUpdated();
    }

    public void changeGradeNumber(GradeNumber gradeNumber) {
        if (gradeNumber == null) {
            throw new DomainValidationException("Grade number is required");
        }
        this.gradeNumber = gradeNumber;
        markUpdated();
    }

    public void changeHomeroomTeacherName(String homeroomTeacherName) {
        this.homeroomTeacherName = normalizeOptional(homeroomTeacherName, 150, "Homeroom teacher name");
        markUpdated();
    }

    public void changeRoomName(String roomName) {
        this.roomName = normalizeOptional(roomName, 50, "Room name");
        markUpdated();
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
