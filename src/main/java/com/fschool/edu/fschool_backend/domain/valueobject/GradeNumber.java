package com.fschool.edu.fschool_backend.domain.valueobject;

import com.fschool.edu.fschool_backend.domain.exception.DomainValidationException;

public record GradeNumber(short value) {

    public GradeNumber {
        if (value != 10 && value != 11 && value != 12) {
            throw new DomainValidationException("Grade number must be 10, 11 or 12");
        }
    }
}
