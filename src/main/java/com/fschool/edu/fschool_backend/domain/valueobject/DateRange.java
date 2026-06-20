package com.fschool.edu.fschool_backend.domain.valueobject;

import com.fschool.edu.fschool_backend.domain.exception.DomainValidationException;
import java.time.LocalDate;

public record DateRange(LocalDate startDate, LocalDate endDate) {

    public DateRange {
        if (startDate == null) {
            throw new DomainValidationException("Start date is required");
        }
        if (endDate == null) {
            throw new DomainValidationException("End date is required");
        }
        if (!endDate.isAfter(startDate)) {
            throw new DomainValidationException("End date must be after start date");
        }
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }
}
