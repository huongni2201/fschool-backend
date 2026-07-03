package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record GradeSubjectDetailResponse(
        GradeSummaryResponse.Period period,
        GradeSummaryResponse.SubjectSummary subject,
        List<ComponentScore> componentScores) {

    public record ComponentScore(
            String id,
            String label,
            BigDecimal value,
            BigDecimal coefficient,
            LocalDate date) {
    }
}
