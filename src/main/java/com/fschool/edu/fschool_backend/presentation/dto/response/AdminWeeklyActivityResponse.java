package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.time.LocalDate;

public record AdminWeeklyActivityResponse(
        LocalDate date,
        long attendanceCount,
        long submittedAssignmentCount,
        long notificationCount) {
}
