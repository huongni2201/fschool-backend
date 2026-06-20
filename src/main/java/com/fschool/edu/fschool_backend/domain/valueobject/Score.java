package com.fschool.edu.fschool_backend.domain.valueobject;

import com.fschool.edu.fschool_backend.domain.exception.DomainValidationException;
import java.math.BigDecimal;

public record Score(BigDecimal value, BigDecimal maxScore) {

    public Score {
        if (maxScore == null || maxScore.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainValidationException("Max score must be greater than zero");
        }
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainValidationException("Score cannot be negative");
        }
        if (value != null && value.compareTo(maxScore) > 0) {
            throw new DomainValidationException("Score cannot be greater than max score");
        }
    }
}
