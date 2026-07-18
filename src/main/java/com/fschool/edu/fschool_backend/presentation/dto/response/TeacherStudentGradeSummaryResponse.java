package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record TeacherStudentGradeSummaryResponse(
        TeacherAcademicPeriodsResponse.Period period,
        BigDecimal overallAverage,
        List<SubjectGrade> subjects) {

    public record SubjectGrade(
            String subjectId,
            String subjectName,
            String teacherName,
            BigDecimal average,
            List<ComponentScore> componentScores) {
    }

    public record ComponentScore(
            String label,
            BigDecimal score) {
    }
}
