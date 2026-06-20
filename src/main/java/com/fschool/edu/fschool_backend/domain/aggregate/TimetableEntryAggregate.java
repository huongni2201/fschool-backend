package com.fschool.edu.fschool_backend.domain.aggregate;

import com.fschool.edu.fschool_backend.domain.exception.DomainValidationException;
import com.fschool.edu.fschool_backend.domain.valueobject.AuditInfo;
import com.fschool.edu.fschool_backend.domain.valueobject.EntityId;
import com.fschool.edu.fschool_backend.domain.valueobject.PeriodNumber;
import com.fschool.edu.fschool_backend.domain.valueobject.SchoolDay;
import com.fschool.edu.fschool_backend.domain.valueobject.TimeRange;
import lombok.Getter;

@Getter
public class TimetableEntryAggregate extends AuditableAggregateRoot {

    private final EntityId classId;
    private final EntityId semesterId;
    private final EntityId subjectId;
    private SchoolDay dayOfWeek;
    private PeriodNumber periodNo;
    private TimeRange timeRange;
    private String teacherName;
    private String roomName;

    public TimetableEntryAggregate(
            EntityId id,
            EntityId classId,
            EntityId semesterId,
            EntityId subjectId,
            SchoolDay dayOfWeek,
            PeriodNumber periodNo,
            TimeRange timeRange,
            String teacherName,
            String roomName,
            AuditInfo auditInfo) {
        super(id, auditInfo);
        if (classId == null || semesterId == null || subjectId == null) {
            throw new DomainValidationException("Class, semester and subject ids are required");
        }
        this.classId = classId;
        this.semesterId = semesterId;
        this.subjectId = subjectId;
        reschedule(dayOfWeek, periodNo, timeRange);
        changeTeacherName(teacherName);
        changeRoomName(roomName);
    }

    public void reschedule(SchoolDay dayOfWeek, PeriodNumber periodNo, TimeRange timeRange) {
        if (dayOfWeek == null || periodNo == null || timeRange == null) {
            throw new DomainValidationException("Day, period and time range are required");
        }
        this.dayOfWeek = dayOfWeek;
        this.periodNo = periodNo;
        this.timeRange = timeRange;
        markUpdated();
    }

    public void changeTeacherName(String teacherName) {
        this.teacherName = normalizeOptional(teacherName, 150, "Teacher name");
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
