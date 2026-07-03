package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record StudentDashboardResponse(
        StudentInfo student,
        String todayTitle,
        int totalLessonsToday,
        CurrentLesson currentLesson,
        List<ScheduleItem> todaySchedule,
        List<RecentGrade> recentGrades) {

    public record StudentInfo(String fullName, String studentCode) {
    }

    public record CurrentLesson(
            String subjectName,
            String periodLabel,
            String roomName,
            String teacherName,
            String statusLabel) {
    }

    public record ScheduleItem(
            String timeLabel,
            String subjectName,
            String periodLabel,
            String roomName,
            String teacherName,
            String status,
            String statusLabel) {
    }

    public record RecentGrade(String subjectName, BigDecimal score, BigDecimal maxScore, String label) {
    }
}
