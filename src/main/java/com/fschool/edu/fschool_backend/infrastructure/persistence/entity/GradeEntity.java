package com.fschool.edu.fschool_backend.infrastructure.persistence.entity;

import com.fschool.edu.fschool_backend.domain.enums.GradeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "grades")
public class GradeEntity extends AuditableEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(name = "semester_id", nullable = false)
    private UUID semesterId;

    @Column(nullable = false, length = 150)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade_type", nullable = false, length = 30)
    private GradeType gradeType;

    @Column(precision = 4, scale = 2)
    private BigDecimal score;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal weight;

    @Column(name = "max_score", nullable = false, precision = 4, scale = 2)
    private BigDecimal maxScore;

    @Column(length = 255)
    private String comment;

    @Column(name = "assessment_date")
    private LocalDate assessmentDate;
}
