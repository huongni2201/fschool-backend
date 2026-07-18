package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.util.List;

public record TeacherAcademicPeriodsResponse(
        List<Period> periods) {

    public record Period(
            String id,
            String label,
            String title,
            String schoolYear) {
    }
}
