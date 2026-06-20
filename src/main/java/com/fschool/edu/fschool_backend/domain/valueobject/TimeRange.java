package com.fschool.edu.fschool_backend.domain.valueobject;

import com.fschool.edu.fschool_backend.domain.exception.DomainValidationException;
import java.time.LocalTime;

public record TimeRange(LocalTime startTime, LocalTime endTime) {

    public TimeRange {
        if (startTime == null) {
            throw new DomainValidationException("Start time is required");
        }
        if (endTime == null) {
            throw new DomainValidationException("End time is required");
        }
        if (!endTime.isAfter(startTime)) {
            throw new DomainValidationException("End time must be after start time");
        }
    }
}
