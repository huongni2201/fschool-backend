package com.fschool.edu.fschool_backend.presentation.controller;

import com.fschool.edu.fschool_backend.application.service.TeacherMobileService;
import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.TeacherAcademicPeriodsResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.TeacherClassStudentsResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.TeacherNotificationsResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.TeacherProfileResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.TeacherStudentGradeSubjectDetailResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.TeacherStudentGradeSummaryResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teachers/me")
@PreAuthorize("hasAnyRole('TEACHER', 'HOMEROOM_TEACHER')")
@RequiredArgsConstructor
public class TeacherMobileController {

    private final TeacherMobileService teacherMobileService;

    @GetMapping("/profile")
    ApiResponse<TeacherProfileResponse> profile(Authentication authentication) {
        return ApiResponse.ok(teacherMobileService.getProfile(currentUserId(authentication)));
    }

    @GetMapping("/classes/{classId}/students")
    ApiResponse<TeacherClassStudentsResponse> classStudents(
            Authentication authentication,
            @PathVariable UUID classId) {
        return ApiResponse.ok(teacherMobileService.getClassStudents(currentUserId(authentication), classId));
    }

    @GetMapping("/students/{studentId}/academic-periods")
    ApiResponse<TeacherAcademicPeriodsResponse> academicPeriods(
            Authentication authentication,
            @PathVariable UUID studentId) {
        return ApiResponse.ok(teacherMobileService.getAcademicPeriods(currentUserId(authentication), studentId));
    }

    @GetMapping("/students/{studentId}/grades/summary")
    ApiResponse<TeacherStudentGradeSummaryResponse> gradeSummary(
            Authentication authentication,
            @PathVariable UUID studentId,
            @RequestParam(required = false) String periodId) {
        return ApiResponse.ok(teacherMobileService.getGradeSummary(
                currentUserId(authentication),
                studentId,
                periodId));
    }

    @GetMapping("/students/{studentId}/grades/subjects/{subjectId}")
    ApiResponse<TeacherStudentGradeSubjectDetailResponse> gradeSubjectDetail(
            Authentication authentication,
            @PathVariable UUID studentId,
            @PathVariable String subjectId,
            @RequestParam(required = false) String periodId) {
        return ApiResponse.ok(teacherMobileService.getGradeSubjectDetail(
                currentUserId(authentication),
                studentId,
                subjectId,
                periodId));
    }

    @GetMapping("/notifications")
    ApiResponse<TeacherNotificationsResponse> notifications(Authentication authentication) {
        return ApiResponse.ok(teacherMobileService.getNotifications(currentUserId(authentication)));
    }

    private UUID currentUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
