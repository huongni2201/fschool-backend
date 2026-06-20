package com.fschool.edu.fschool_backend.presentation.dto.response;

import com.fschool.edu.fschool_backend.domain.enums.ExamType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ExamResponse(
        UUID id,
        String title,
        String subjectName,
        ExamType examType,
        LocalDate examDate,
        LocalTime startTime,
        Integer durationMinutes,
        String roomName,
        String note) {
}
