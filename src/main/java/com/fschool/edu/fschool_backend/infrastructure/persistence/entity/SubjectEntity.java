package com.fschool.edu.fschool_backend.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(name = "accent_color", nullable = false, length = 20)
    private String accentColor = "#64748B";

    @Column(name = "is_score_based", nullable = false)
    private Boolean scoreBased = true;
}
