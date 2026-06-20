package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record SchoolYearResponse(UUID id, String name, LocalDate startDate, LocalDate endDate, Boolean isCurrent) {
}
