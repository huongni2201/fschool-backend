package com.fschool.edu.fschool_backend.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "timetable_entries")
public class TimetableEntryEntity extends AuditableEntity {

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Column(name = "semester_id", nullable = false)
    private UUID semesterId;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(name = "day_of_week", nullable = false)
    private Short dayOfWeek;

    @Column(name = "period_no", nullable = false)
    private Short periodNo;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "teacher_name", length = 150)
    private String teacherName;

    @Column(name = "room_name", length = 50)
    private String roomName;
}
