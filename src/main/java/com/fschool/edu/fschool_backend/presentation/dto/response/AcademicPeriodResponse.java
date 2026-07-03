package com.fschool.edu.fschool_backend.presentation.dto.response;

public record AcademicPeriodResponse(
        String id,
        String label,
        String title,
        String schoolYear,
        String type,
        int order) {
}
