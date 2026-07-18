package com.fschool.edu.fschool_backend.infrastructure.persistence.entity;

import com.fschool.edu.fschool_backend.domain.enums.ClassStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "classes")
public class ClassEntity extends AuditableEntity {

    @Column(name = "school_year_id", nullable = false)
    private UUID schoolYearId;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(name = "grade_number", nullable = false)
    private Short gradeNumber;

    @Column(name = "homeroom_teacher_name", length = 150)
    private String homeroomTeacherName;

    @Column(name = "homeroom_teacher_id")
    private UUID homeroomTeacherId;

    @Column(name = "room_name", length = 50)
    private String roomName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClassStatus status = ClassStatus.ACTIVE;
}
