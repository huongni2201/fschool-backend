package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.util.List;

public record AdminDashboardResponse(
        String schoolYearName,
        String semesterName,
        long studentCount,
        long teacherCount,
        long classCount,
        long unreadNotificationCount,
        long submittedRequestCount,
        long gradeRecordCount,
        List<UpcomingEvent> upcomingEvents,
        List<RecentActivity> recentActivities) {

    public record UpcomingEvent(String title, String description, String timeLabel) {
    }

    public record RecentActivity(String title, String description, String timeLabel, String type) {
    }
}
