package com.fschool.edu.fschool_backend.presentation.controller;

import com.fschool.edu.fschool_backend.application.service.StudentPortalService;
import com.fschool.edu.fschool_backend.infrastructure.security.TokenService;
import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentDashboardResponse;
import com.fschool.edu.fschool_backend.presentation.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/students/me")
public class StudentDashboardController {

    private static final String DASHBOARD_ERROR_MESSAGE = "Không tải được dữ liệu trang chủ";
    private static final String SESSION_EXPIRED_MESSAGE = "Phiên đăng nhập đã hết hạn";

    private final TokenService tokenService;
    private final StudentPortalService portalService;

    public StudentDashboardController(TokenService tokenService, StudentPortalService portalService) {
        this.tokenService = tokenService;
        this.portalService = portalService;
    }

    @GetMapping("/dashboard")
    ResponseEntity<ApiResponse<StudentDashboardResponse>> dashboard(
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(
                    portalService.getStudentDashboard(tokenService.requireUser(authorization).id())));
        } catch (ApiException exception) {
            HttpStatus status = exception.getStatus();
            String message = isAuthError(status) ? SESSION_EXPIRED_MESSAGE : DASHBOARD_ERROR_MESSAGE;
            return ResponseEntity.status(status).body(ApiResponse.error(message));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(DASHBOARD_ERROR_MESSAGE));
        }
    }

    private boolean isAuthError(HttpStatus status) {
        return status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN;
    }
}
