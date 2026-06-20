package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.util.List;

public record HomeSummaryResponse(
        HomeStudentResponse student,
        CurrentSemesterResponse currentSemester,
        LessonResponse currentLesson,
        List<TimetableDayResponse> todayTimetable,
        List<GradeItemResponse> recentGrades,
        List<AssignmentResponse> upcomingAssignments,
        List<ExamResponse> upcomingExams,
        List<NewsListItemResponse> latestNews,
        long unreadNotificationCount) {
}
