package com.fschool.edu.fschool_backend.domain.aggregate;

import com.fschool.edu.fschool_backend.domain.enums.GradeType;
import com.fschool.edu.fschool_backend.domain.exception.DomainValidationException;
import com.fschool.edu.fschool_backend.domain.valueobject.AuditInfo;
import com.fschool.edu.fschool_backend.domain.valueobject.EntityId;
import com.fschool.edu.fschool_backend.domain.valueobject.Score;
import com.fschool.edu.fschool_backend.domain.valueobject.Weight;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;

@Getter
public class GradeAggregate extends AuditableAggregateRoot {

    private final EntityId userId;
    private final EntityId subjectId;
    private final EntityId semesterId;
    private String title;
    private GradeType gradeType;
    private Score score;
    private Weight weight;
    private String comment;
    private LocalDate assessmentDate;

    public GradeAggregate(
            EntityId id,
            EntityId userId,
            EntityId subjectId,
            EntityId semesterId,
            String title,
            GradeType gradeType,
            Score score,
            Weight weight,
            String comment,
            LocalDate assessmentDate,
            AuditInfo auditInfo) {
        super(id, auditInfo);
        if (userId == null || subjectId == null || semesterId == null) {
            throw new DomainValidationException("User, subject and semester ids are required");
        }
        this.userId = userId;
        this.subjectId = subjectId;
        this.semesterId = semesterId;
        rename(title);
        changeGradeType(gradeType);
        this.score = score == null ? new Score(null, BigDecimal.TEN) : score;
        this.weight = weight == null ? Weight.one() : weight;
        changeComment(comment);
        this.assessmentDate = assessmentDate;
    }

    public void rename(String title) {
        title = title == null ? "" : title.trim();
        if (title.isBlank() || title.length() > 150) {
            throw new DomainValidationException("Grade title is required and must not exceed 150 characters");
        }
        this.title = title;
        markUpdated();
    }

    public void changeGradeType(GradeType gradeType) {
        if (gradeType == null) {
            throw new DomainValidationException("Grade type is required");
        }
        this.gradeType = gradeType;
        markUpdated();
    }

    public void recordScore(Score score) {
        if (score == null) {
            throw new DomainValidationException("Score is required");
        }
        this.score = score;
        markUpdated();
    }

    public void clearScore() {
        this.score = new Score(null, score.maxScore());
        markUpdated();
    }

    public void changeWeight(Weight weight) {
        if (weight == null) {
            throw new DomainValidationException("Weight is required");
        }
        this.weight = weight;
        markUpdated();
    }

    public void changeComment(String comment) {
        if (comment == null || comment.isBlank()) {
            this.comment = null;
        } else if (comment.length() > 255) {
            throw new DomainValidationException("Comment must not exceed 255 characters");
        } else {
            this.comment = comment.trim();
        }
        markUpdated();
    }
}
