package com.fschool.edu.fschool_backend.presentation.controller;

import com.fschool.edu.fschool_backend.application.service.AdminDashboardService;
import com.fschool.edu.fschool_backend.presentation.dto.response.AdminDashboardResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    @GetMapping("/dashboard")
    ApiResponse<AdminDashboardResponse> dashboard() {
        return ApiResponse.ok(dashboardService.getDashboard());
    }
}
