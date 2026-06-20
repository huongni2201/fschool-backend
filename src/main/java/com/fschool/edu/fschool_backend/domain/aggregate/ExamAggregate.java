package com.fschool.edu.fschool_backend.domain.aggregate;

import com.fschool.edu.fschool_backend.domain.enums.ExamType;
import com.fschool.edu.fschool_backend.domain.exception.DomainValidationException;
import com.fschool.edu.fschool_backend.domain.valueobject.AuditInfo;
import com.fschool.edu.fschool_backend.domain.valueobject.EntityId;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;

@Getter
public class ExamAggregate extends AuditableAggregateRoot {

    private final EntityId classId;
    private final EntityId subjectId;
    private final EntityId semesterId;
    private String title;
    private ExamType examType;
    private LocalDate examDate;
    private LocalTime startTime;
    private int durationMinutes;
    private String roomName;
    private String note;

    public ExamAggregate(
            EntityId id,
            EntityId classId,
            EntityId subjectId,
            EntityId semesterId,
            String title,
            ExamType examType,
            LocalDate examDate,
            LocalTime startTime,
            int durationMinutes,
            String roomName,
            String note,
            AuditInfo auditInfo) {
        super(id, auditInfo);
        if (classId == null || subjectId == null || semesterId == null) {
            throw new DomainValidationException("Class, subject and semester ids are required");
        }
        this.classId = classId;
        this.subjectId = subjectId;
        this.semesterId = semesterId;
        rename(title);
        changeExamType(examType);
        reschedule(examDate, startTime, durationMinutes);
        this.roomName = normalizeOptional(roomName, 50, "Room name");
        this.note = normalizeOptional(note, 255, "Note");
    }

    public void rename(String title) {
        title = title == null ? "" : title.trim();
        if (title.isBlank() || title.length() > 200) {
            throw new DomainValidationException("Exam title is required and must not exceed 200 characters");
        }
        this.title = title;
        markUpdated();
    }

    public void changeExamType(ExamType examType) {
        if (examType == null) {
            throw new DomainValidationException("Exam type is required");
        }
        this.examType = examType;
        markUpdated();
    }

    public void reschedule(LocalDate examDate, LocalTime startTime, int durationMinutes) {
        if (examDate == null) {
            throw new DomainValidationException("Exam date is required");
        }
        if (startTime == null) {
            throw new DomainValidationException("Exam start time is required");
        }
        if (durationMinutes <= 0) {
            throw new DomainValidationException("Exam duration must be greater than zero");
        }
        this.examDate = examDate;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
        markUpdated();
    }

    public void changeRoomName(String roomName) {
        this.roomName = normalizeOptional(roomName, 50, "Room name");
        markUpdated();
    }

    public void changeNote(String note) {
        this.note = normalizeOptional(note, 255, "Note");
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
