package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.time.LocalDate;
import java.util.List;

public record TimetableDayResponse(Short dayOfWeek, LocalDate date, List<LessonResponse> lessons) {
}
