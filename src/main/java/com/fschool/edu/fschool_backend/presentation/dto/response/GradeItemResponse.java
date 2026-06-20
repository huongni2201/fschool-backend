package com.fschool.edu.fschool_backend.presentation.dto.response;

import com.fschool.edu.fschool_backend.domain.enums.GradeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record GradeItemResponse(
        UUID id,
        String title,
        GradeType gradeType,
        BigDecimal score,
        BigDecimal maxScore,
        BigDecimal weight,
        LocalDate assessmentDate,
        String comment) {
}
