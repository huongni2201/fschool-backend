package com.fschool.edu.fschool_backend.domain.valueobject;

import com.fschool.edu.fschool_backend.domain.exception.DomainValidationException;
import java.math.BigDecimal;

public record Weight(BigDecimal value) {

    public Weight {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainValidationException("Weight must be greater than zero");
        }
    }

    public static Weight one() {
        return new Weight(BigDecimal.ONE);
    }
}
