package com.fschool.edu.fschool_backend.presentation.controller;

import com.fschool.edu.fschool_backend.application.service.StudentPortalService;
import com.fschool.edu.fschool_backend.presentation.dto.response.ApiErrorResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentTuitionResponse;
import com.fschool.edu.fschool_backend.presentation.exception.ApiException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/students/me/tuition", "/students/me/tuition"})
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class StudentTuitionController {

    private static final String TUITION_LOAD_FAILED_MESSAGE =
            "Kh\u00F4ng th\u1EC3 t\u1EA3i d\u1EEF li\u1EC7u h\u1ECDc ph\u00ED";

    private final StudentPortalService portalService;

    @GetMapping
    ResponseEntity<?> tuition(
            Authentication authentication) {
        try {
            UUID studentId = currentUserId(authentication);
            return ResponseEntity.ok(ApiResponse.ok(portalService.getStudentTuition(studentId)));
        } catch (ApiException exception) {
            return ResponseEntity.status(exception.getStatus()).body(tuitionLoadFailed());
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(tuitionLoadFailed());
        }
    }

    private ApiErrorResponse tuitionLoadFailed() {
        return new ApiErrorResponse(false, TUITION_LOAD_FAILED_MESSAGE, null, null);
    }

    private UUID currentUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
