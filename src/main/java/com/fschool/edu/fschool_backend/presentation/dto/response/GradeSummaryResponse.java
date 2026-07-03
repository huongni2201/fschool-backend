package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record GradeSummaryResponse(
        Period period,
        BigDecimal overallAverage,
        String rankLabel,
        int subjectCount,
        int excellentSubjectCount,
        SubjectAverage strongestSubject,
        List<SubjectSummary> subjects) {

    public record Period(String id, String label, String title, String schoolYear) {
    }

    public record SubjectAverage(String subjectId, String subjectName, BigDecimal average) {
    }

    public record SubjectSummary(
            String subjectId,
            String subjectName,
            String teacherName,
            String group,
            BigDecimal average,
            String rankLabel,
            String accentColor) {
    }
}
