package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.time.LocalDate;
import java.util.List;

public record StudentTimetableResponse(
        LocalDate weekStart,
        LocalDate weekEnd,
        List<Day> days) {

    public record Day(
            LocalDate date,
            String label,
            List<Lesson> lessons) {
    }

    public record Lesson(
            String id,
            String subjectName,
            String className,
            String roomName,
            String teacherName,
            Short period,
            String periodLabel,
            String startTime,
            String endTime,
            String status,
            String statusLabel,
            String note) {
    }
}
