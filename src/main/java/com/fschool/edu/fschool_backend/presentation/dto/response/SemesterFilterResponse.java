package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record SemesterFilterResponse(
        UUID id,
        String name,
        UUID academicYearId,
        LocalDate startDate,
        LocalDate endDate) {
}
