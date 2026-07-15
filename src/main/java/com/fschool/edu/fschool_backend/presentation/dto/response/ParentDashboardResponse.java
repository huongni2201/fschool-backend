package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ParentDashboardResponse(
        Parent parent,
        long unreadNotificationCount,
        Instant lastUpdatedAt,
        Student student,
        List<Alert> alerts) {

    public record Parent(String id, String fullName) {
    }

    public record Student(
            String id,
            String studentCode,
            String fullName,
            String className,
            String avatarText,
            BigDecimal gradeAverage,
            Tuition tuition,
            NextLesson nextLesson,
            String statusLabel,
            HomeroomTeacher homeroomTeacher,
            List<Alert> alerts) {
    }

    public record Tuition(String statusLabel, long remainingAmount) {
    }

    public record NextLesson(String subjectName, String periodLabel, String timeLabel) {
    }

    public record HomeroomTeacher(String fullName, String role, String phone, String email) {
    }

    public record Alert(String type, String title, String message) {
    }
}
