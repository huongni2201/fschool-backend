package com.fschool.edu.fschool_backend.presentation.controller;

import com.fschool.edu.fschool_backend.application.service.StudentProfileService;
import com.fschool.edu.fschool_backend.presentation.dto.request.NotificationSettingsRequest;
import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.NotificationSettingsResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/students/me/notification-settings", "/students/me/notification-settings"})
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class StudentNotificationSettingsController {

    private static final String SAVE_SUCCESS_MESSAGE =
            "\u0110\u00E3 l\u01B0u c\u00E0i \u0111\u1EB7t th\u00F4ng b\u00E1o";

    private final StudentProfileService profileService;

    @GetMapping
    ApiResponse<NotificationSettingsResponse> getSettings(
            Authentication authentication) {
        UUID studentId = currentUserId(authentication);
        return ApiResponse.ok(profileService.getNotificationSettings(studentId));
    }

    @PutMapping
    ApiResponse<NotificationSettingsResponse> updateSettings(
            Authentication authentication,
            @RequestBody(required = false) NotificationSettingsRequest request) {
        UUID studentId = currentUserId(authentication);
        NotificationSettingsResponse current = profileService.getNotificationSettings(studentId);
        NotificationSettingsResponse updated = request == null
                ? current
                : request.mergeWith(current);
        return ApiResponse.ok(profileService.saveNotificationSettings(studentId, updated), SAVE_SUCCESS_MESSAGE);
    }

    private UUID currentUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
