package com.fschool.edu.fschool_backend.domain.valueobject;

import com.fschool.edu.fschool_backend.domain.exception.DomainValidationException;

public record SchoolDay(short value) {

    public SchoolDay {
        if (value < 1 || value > 7) {
            throw new DomainValidationException("School day must be between 1 and 7");
        }
    }
}
