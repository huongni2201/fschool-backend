package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record TeacherStudentGradeSubjectDetailResponse(
        String subjectId,
        String subjectName,
        String teacherName,
        BigDecimal average,
        List<TeacherStudentGradeSummaryResponse.ComponentScore> componentScores) {
}
