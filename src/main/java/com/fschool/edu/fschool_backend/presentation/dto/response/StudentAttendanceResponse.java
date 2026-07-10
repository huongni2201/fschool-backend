package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.time.LocalDate;
import java.util.List;

public record StudentAttendanceResponse(
        Summary summary,
        List<Item> items) {

    public record Summary(
            int totalSessions,
            int present,
            int absent,
            int late,
            int excused) {
    }

    public record Item(
            String id,
            LocalDate date,
            String subjectName,
            String period,
            String timeRange,
            String teacherName,
            String status,
            String statusLabel,
            String note) {
    }
}
