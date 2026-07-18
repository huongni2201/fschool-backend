package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.util.List;
import java.util.UUID;

public record AdminTimetableResponse(
        AcademicYear academicYear,
        Semester semester,
        ClassInfo classInfo,
        List<Day> days) {

    public static AdminTimetableResponse empty() {
        return new AdminTimetableResponse(null, null, null, List.of());
    }

    public record AcademicYear(UUID id, String name) {
    }

    public record Semester(UUID id, String name) {
    }

    public record ClassInfo(UUID id, String name, int gradeLevel) {
    }

    public record Day(
            int dayOfWeek,
            String dayName,
            List<Lesson> lessons) {
    }

    public record Lesson(
            int period,
            String startTime,
            String endTime,
            UUID subjectId,
            String subjectName,
            UUID teacherId,
            String teacherName,
            String room,
            UUID classId,
            String className,
            Integer gradeLevel) {
    }
}
