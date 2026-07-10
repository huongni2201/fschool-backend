package com.fschool.edu.fschool_backend.presentation.controller;

import com.fschool.edu.fschool_backend.application.service.StudentProfileService;
import com.fschool.edu.fschool_backend.domain.enums.UserRole;
import com.fschool.edu.fschool_backend.infrastructure.security.CurrentUser;
import com.fschool.edu.fschool_backend.infrastructure.security.TokenService;
import com.fschool.edu.fschool_backend.presentation.dto.request.NotificationSettingsRequest;
import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.NotificationSettingsResponse;
import com.fschool.edu.fschool_backend.presentation.exception.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/students/me/notification-settings", "/students/me/notification-settings"})
public class StudentNotificationSettingsController {

    private static final String SAVE_SUCCESS_MESSAGE =
            "\u0110\u00E3 l\u01B0u c\u00E0i \u0111\u1EB7t th\u00F4ng b\u00E1o";

    private final TokenService tokenService;
    private final StudentProfileService profileService;

    public StudentNotificationSettingsController(TokenService tokenService, StudentProfileService profileService) {
        this.tokenService = tokenService;
        this.profileService = profileService;
    }

    @GetMapping
    ApiResponse<NotificationSettingsResponse> getSettings(
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        UUID studentId = requireStudentId(authorization);
        return ApiResponse.ok(profileService.getNotificationSettings(studentId));
    }

    @PutMapping
    ApiResponse<NotificationSettingsResponse> updateSettings(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestBody(required = false) NotificationSettingsRequest request) {
        UUID studentId = requireStudentId(authorization);
        NotificationSettingsResponse current = profileService.getNotificationSettings(studentId);
        NotificationSettingsResponse updated = request == null
                ? current
                : request.mergeWith(current);
        return ApiResponse.ok(profileService.saveNotificationSettings(studentId, updated), SAVE_SUCCESS_MESSAGE);
    }

    private UUID requireStudentId(String authorization) {
        CurrentUser currentUser = tokenService.requireUser(authorization);
        if (currentUser.role() != UserRole.STUDENT) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Notification settings are only available for students");
        }
        return currentUser.id();
    }
}
