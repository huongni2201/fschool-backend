package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SubjectGradesResponse(
        UUID subjectId,
        String subjectCode,
        String subjectName,
        BigDecimal averageScore,
        List<GradeItemResponse> grades) {
}
