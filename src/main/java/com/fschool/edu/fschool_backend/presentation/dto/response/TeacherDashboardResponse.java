package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.util.List;

public record TeacherDashboardResponse(
        Teacher teacher,
        List<TodayClass> todayClasses,
        List<ManagedClass> managedClasses,
        HomeroomClass homeroomClass,
        long pendingApplications,
        List<UpcomingExam> upcomingExams,
        List<RecentNotification> recentNotifications,
        List<Task> tasks) {

    public record Teacher(
            String id,
            String fullName,
            String employeeCode,
            String departmentName) {
    }

    public record TodayClass(
            String classId,
            String className,
            String subjectId,
            String subjectName,
            String room,
            String timeLabel,
            String statusLabel) {
    }

    public record ManagedClass(
            String id,
            String name,
            String roleLabel,
            String subjectName,
            long studentCount) {
    }

    public record HomeroomClass(
            String id,
            String name,
            String roleLabel,
            long studentCount) {
    }

    public record UpcomingExam(
            String title,
            String className,
            String subjectName,
            String dateLabel) {
    }

    public record RecentNotification(
            String title,
            String message,
            String type) {
    }

    public record Task(
            String title,
            String message,
            long count,
            String type) {
    }
}
