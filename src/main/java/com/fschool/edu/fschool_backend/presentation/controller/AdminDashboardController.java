package com.fschool.edu.fschool_backend.presentation.controller;

import com.fschool.edu.fschool_backend.application.service.AdminDashboardService;
import com.fschool.edu.fschool_backend.presentation.dto.response.AdminDashboardResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.AdminDashboardSummaryResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.AdminRecentImportResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.AdminRecentNotificationResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.AdminWeeklyActivityResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    @GetMapping("/admin/dashboard")
    ApiResponse<AdminDashboardResponse> dashboard() {
        return ApiResponse.ok(dashboardService.getDashboard());
    }

    @GetMapping("/dashboard/summary")
    ApiResponse<AdminDashboardSummaryResponse> summary() {
        return ApiResponse.ok(dashboardService.getSummary());
    }

    @GetMapping("/dashboard/recent-notifications")
    ApiResponse<List<AdminRecentNotificationResponse>> recentNotifications(
            @RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.ok(dashboardService.getRecentNotifications(limit));
    }

    @GetMapping("/dashboard/weekly-activity")
    ApiResponse<List<AdminWeeklyActivityResponse>> weeklyActivity(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(dashboardService.getWeeklyActivity(from, to));
    }

    @GetMapping("/dashboard/recent-imports")
    ApiResponse<List<AdminRecentImportResponse>> recentImports(
            @RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.ok(dashboardService.getRecentImports(limit));
    }
}
