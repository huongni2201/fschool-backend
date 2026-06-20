package com.fschool.edu.fschool_backend.domain.valueobject;

import com.fschool.edu.fschool_backend.domain.exception.DomainValidationException;

public record PersonName(String value) {

    public PersonName {
        value = value == null ? "" : value.trim();
        if (value.isBlank()) {
            throw new DomainValidationException("Name is required");
        }
        if (value.length() > 150) {
            throw new DomainValidationException("Name must not exceed 150 characters");
        }
    }
}
