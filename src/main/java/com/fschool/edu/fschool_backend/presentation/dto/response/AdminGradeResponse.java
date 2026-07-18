package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record AdminGradeResponse(
        UUID studentId,
        String studentCode,
        String fullName,
        BigDecimal score15Min,
        BigDecimal scoreOnePeriod,
        BigDecimal semesterScore,
        BigDecimal averageScore,
        String classification) {
}
