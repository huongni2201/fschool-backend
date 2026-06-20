package com.fschool.edu.fschool_backend.presentation.controller;

import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.CountResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.NotificationResponse;
import com.fschool.edu.fschool_backend.infrastructure.security.TokenService;
import com.fschool.edu.fschool_backend.application.service.StudentPortalService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final TokenService tokenService;
    private final StudentPortalService portalService;

    public NotificationController(TokenService tokenService, StudentPortalService portalService) {
        this.tokenService = tokenService;
        this.portalService = portalService;
    }

    @GetMapping
    ApiResponse<List<NotificationResponse>> notifications(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestParam(required = false) Boolean isRead) {
        return ApiResponse.ok(portalService.getNotifications(tokenService.requireUser(authorization).id(), isRead));
    }

    @GetMapping("/unread-count")
    ApiResponse<CountResponse> unreadCount(
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        return ApiResponse.ok(portalService.getUnreadNotificationCount(tokenService.requireUser(authorization).id()));
    }

    @PatchMapping("/{id}/read")
    ApiResponse<Void> markRead(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable UUID id) {
        portalService.markNotificationRead(tokenService.requireUser(authorization).id(), id);
        return ApiResponse.ok();
    }

    @PatchMapping("/read-all")
    ApiResponse<Void> markAllRead(@RequestHeader(name = "Authorization", required = false) String authorization) {
        portalService.markAllNotificationsRead(tokenService.requireUser(authorization).id());
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    ApiResponse<Void> delete(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable UUID id) {
        portalService.deleteNotification(tokenService.requireUser(authorization).id(), id);
        return ApiResponse.ok();
    }
}
