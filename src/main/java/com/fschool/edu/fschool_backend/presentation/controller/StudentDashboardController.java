package com.fschool.edu.fschool_backend.presentation.controller;

import com.fschool.edu.fschool_backend.application.service.StudentPortalService;
import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentDashboardResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentTimetableResponse;
import com.fschool.edu.fschool_backend.presentation.exception.ApiException;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/students/me")
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class StudentDashboardController {

    private static final String TIMETABLE_ERROR_MESSAGE = "Kh\u00F4ng th\u1EC3 t\u1EA3i th\u1EDDi kho\u00E1 bi\u1EC3u";

    private static final String DASHBOARD_ERROR_MESSAGE = "Không tải được dữ liệu trang chủ";
    private static final String SESSION_EXPIRED_MESSAGE = "Phiên đăng nhập đã hết hạn";

    private final StudentPortalService portalService;

    @GetMapping("/dashboard")
    ResponseEntity<ApiResponse<StudentDashboardResponse>> dashboard(
            Authentication authentication) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(
                    portalService.getStudentDashboard(currentUserId(authentication))));
        } catch (ApiException exception) {
            HttpStatus status = exception.getStatus();
            String message = isAuthError(status) ? SESSION_EXPIRED_MESSAGE : DASHBOARD_ERROR_MESSAGE;
            return ResponseEntity.status(status).body(ApiResponse.error(message));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(DASHBOARD_ERROR_MESSAGE));
        }
    }

    @GetMapping("/timetable")
    ResponseEntity<ApiResponse<StudentTimetableResponse>> timetable(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(
                    portalService.getStudentTimetable(currentUserId(authentication), startDate, endDate),
                    "OK"));
        } catch (ApiException exception) {
            HttpStatus status = isAuthError(exception.getStatus())
                    ? exception.getStatus()
                    : HttpStatus.INTERNAL_SERVER_ERROR;
            return ResponseEntity.status(status).body(ApiResponse.error(TIMETABLE_ERROR_MESSAGE));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(TIMETABLE_ERROR_MESSAGE));
        }
    }

    private boolean isAuthError(HttpStatus status) {
        return status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN;
    }

    private UUID currentUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
