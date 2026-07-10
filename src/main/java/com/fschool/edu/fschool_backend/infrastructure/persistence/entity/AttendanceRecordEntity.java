package com.fschool.edu.fschool_backend.infrastructure.persistence.entity;

import com.fschool.edu.fschool_backend.domain.enums.AttendanceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "student_attendance_records")
public class AttendanceRecordEntity extends AuditableEntity {

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "class_id")
    private UUID classId;

    @Column(name = "subject_id")
    private UUID subjectId;

    @Column(name = "timetable_entry_id")
    private UUID timetableEntryId;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "period_no")
    private Short periodNo;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "teacher_name", length = 150)
    private String teacherName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus status = AttendanceStatus.UNKNOWN;

    @Column(length = 500)
    private String note;
}
