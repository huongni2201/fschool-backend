package com.fschool.edu.fschool_backend.domain.aggregate;

import com.fschool.edu.fschool_backend.domain.enums.AssignmentStatus;
import com.fschool.edu.fschool_backend.domain.exception.BusinessRuleViolationException;
import com.fschool.edu.fschool_backend.domain.exception.DomainValidationException;
import com.fschool.edu.fschool_backend.domain.valueobject.AuditInfo;
import com.fschool.edu.fschool_backend.domain.valueobject.EntityId;
import java.time.Instant;
import lombok.Getter;

@Getter
public class AssignmentAggregate extends AuditableAggregateRoot {

    private final EntityId classId;
    private final EntityId subjectId;
    private final EntityId semesterId;
    private String title;
    private String description;
    private String teacherName;
    private String attachmentUrl;
    private Instant dueAt;
    private AssignmentStatus status;

    public AssignmentAggregate(
            EntityId id,
            EntityId classId,
            EntityId subjectId,
            EntityId semesterId,
            String title,
            String description,
            String teacherName,
            String attachmentUrl,
            Instant dueAt,
            AssignmentStatus status,
            AuditInfo auditInfo) {
        super(id, auditInfo);
        if (classId == null || subjectId == null || semesterId == null) {
            throw new DomainValidationException("Class, subject and semester ids are required");
        }
        this.classId = classId;
        this.subjectId = subjectId;
        this.semesterId = semesterId;
        rename(title);
        this.description = normalizeOptional(description, 10_000, "Description");
        this.teacherName = normalizeOptional(teacherName, 150, "Teacher name");
        this.attachmentUrl = normalizeOptional(attachmentUrl, 2048, "Attachment URL");
        this.dueAt = dueAt;
        this.status = status == null ? AssignmentStatus.PUBLISHED : status;
    }

    public void rename(String title) {
        title = title == null ? "" : title.trim();
        if (title.isBlank() || title.length() > 200) {
            throw new DomainValidationException("Assignment title is required and must not exceed 200 characters");
        }
        this.title = title;
        markUpdated();
    }

    public void publish() {
        if (status == AssignmentStatus.CLOSED) {
            throw new BusinessRuleViolationException("Closed assignments cannot be published again");
        }
        status = AssignmentStatus.PUBLISHED;
        markUpdated();
    }

    public void close() {
        status = AssignmentStatus.CLOSED;
        markUpdated();
    }

    public void moveToDraft() {
        if (status == AssignmentStatus.CLOSED) {
            throw new BusinessRuleViolationException("Closed assignments cannot be moved to draft");
        }
        status = AssignmentStatus.DRAFT;
        markUpdated();
    }

    public void changeDueAt(Instant dueAt) {
        this.dueAt = dueAt;
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
