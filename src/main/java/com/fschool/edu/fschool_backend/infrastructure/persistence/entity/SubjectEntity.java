package com.fschool.edu.fschool_backend.infrastructure.persistence.entity;

import com.fschool.edu.fschool_backend.domain.enums.SubjectStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "subjects")
public class SubjectEntity extends AuditableEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "subject_group", nullable = false, length = 50)
    private String subjectGroup = "OTHER";

    @Column(name = "is_score_based", nullable = false)
    private Boolean scoreBased = true;

    @Column(name = "grade_levels", nullable = false, length = 20)
    private String gradeLevels = "10,11,12";

    @Column(name = "lessons_per_week", nullable = false)
    private Short lessonsPerWeek = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubjectStatus status = SubjectStatus.ACTIVE;
}
