package com.fschool.edu.fschool_backend.presentation.dto.response;

public record AdminDashboardSummaryResponse(
        long totalStudents,
        long totalTeachers,
        long totalClasses,
        long totalSubjects) {
}
