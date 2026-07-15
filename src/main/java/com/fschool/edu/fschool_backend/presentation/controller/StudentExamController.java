package com.fschool.edu.fschool_backend.presentation.controller;

import com.fschool.edu.fschool_backend.application.service.StudentPortalService;
import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentExamScheduleResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/students/me/exams", "/students/me/exams"})
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class StudentExamController {

    private final StudentPortalService portalService;

    @GetMapping
    ApiResponse<StudentExamScheduleResponse> exams(
            Authentication authentication) {
        UUID studentId = currentUserId(authentication);
        return ApiResponse.ok(portalService.getStudentExamSchedule(studentId), "OK");
    }

    private UUID currentUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
