package com.fschool.edu.fschool_backend.presentation.controller;

import com.fschool.edu.fschool_backend.application.service.StudentPortalService;
import com.fschool.edu.fschool_backend.domain.enums.UserRole;
import com.fschool.edu.fschool_backend.infrastructure.security.CurrentUser;
import com.fschool.edu.fschool_backend.infrastructure.security.TokenService;
import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentNotificationsResponse;
import com.fschool.edu.fschool_backend.presentation.exception.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/students/me/notifications", "/students/me/notifications"})
public class StudentNotificationController {

    private final TokenService tokenService;
    private final StudentPortalService portalService;

    public StudentNotificationController(TokenService tokenService, StudentPortalService portalService) {
        this.tokenService = tokenService;
        this.portalService = portalService;
    }

    @GetMapping
    ApiResponse<StudentNotificationsResponse> notifications(
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        UUID studentId = requireStudentId(authorization);
        return ApiResponse.ok(portalService.getStudentNotifications(studentId), "OK");
    }

    private UUID requireStudentId(String authorization) {
        CurrentUser currentUser = tokenService.requireUser(authorization);
        if (currentUser.role() != UserRole.STUDENT) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Student notifications are only available for students");
        }
        return currentUser.id();
    }
}
