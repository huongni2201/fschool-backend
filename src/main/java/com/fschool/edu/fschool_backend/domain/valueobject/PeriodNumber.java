package com.fschool.edu.fschool_backend.domain.valueobject;

import com.fschool.edu.fschool_backend.domain.exception.DomainValidationException;

public record PeriodNumber(short value) {

    public PeriodNumber {
        if (value < 1 || value > 15) {
            throw new DomainValidationException("Period number must be between 1 and 15");
        }
    }
}
