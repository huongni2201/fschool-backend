package com.fschool.edu.fschool_backend.presentation.controller;

import com.fschool.edu.fschool_backend.application.service.ParentPortalService;
import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.ParentDashboardResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/parents/me", "/parents/me"})
@PreAuthorize("hasRole('PARENT')")
@RequiredArgsConstructor
public class ParentDashboardController {

    private final ParentPortalService parentPortalService;

    @GetMapping("/dashboard")
    ApiResponse<ParentDashboardResponse> dashboard(
            Authentication authentication) {
        return ApiResponse.ok(parentPortalService.getDashboard(currentUserId(authentication)));
    }

    private UUID currentUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
