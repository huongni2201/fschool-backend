package com.fschool.edu.fschool_backend.domain.valueobject;

import com.fschool.edu.fschool_backend.domain.exception.DomainValidationException;

public record StudentCode(String value) {

    public StudentCode {
        value = value == null ? "" : value.trim().toUpperCase();
        if (value.isBlank()) {
            throw new DomainValidationException("Student code is required");
        }
        if (value.length() > 20) {
            throw new DomainValidationException("Student code must not exceed 20 characters");
        }
    }
}
