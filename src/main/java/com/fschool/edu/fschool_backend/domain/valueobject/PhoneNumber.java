package com.fschool.edu.fschool_backend.domain.valueobject;

import com.fschool.edu.fschool_backend.domain.exception.DomainValidationException;

public record PhoneNumber(String value) {

    public PhoneNumber {
        value = normalize(value);
        if (value.isBlank()) {
            throw new DomainValidationException("Phone number is required");
        }
        if (value.length() > 20) {
            throw new DomainValidationException("Phone number must not exceed 20 characters");
        }
        if (!value.matches("\\+?[0-9]{8,20}")) {
            throw new DomainValidationException("Phone number format is invalid");
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replace(" ", "");
    }
}
