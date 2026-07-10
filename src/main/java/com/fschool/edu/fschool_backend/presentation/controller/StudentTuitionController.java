package com.fschool.edu.fschool_backend.presentation.controller;

import com.fschool.edu.fschool_backend.application.service.StudentPortalService;
import com.fschool.edu.fschool_backend.domain.enums.UserRole;
import com.fschool.edu.fschool_backend.infrastructure.security.CurrentUser;
import com.fschool.edu.fschool_backend.infrastructure.security.TokenService;
import com.fschool.edu.fschool_backend.presentation.dto.response.ApiErrorResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentTuitionResponse;
import com.fschool.edu.fschool_backend.presentation.exception.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/students/me/tuition", "/students/me/tuition"})
public class StudentTuitionController {

    private static final String TUITION_LOAD_FAILED_MESSAGE =
            "Kh\u00F4ng th\u1EC3 t\u1EA3i d\u1EEF li\u1EC7u h\u1ECDc ph\u00ED";

    private final TokenService tokenService;
    private final StudentPortalService portalService;

    public StudentTuitionController(TokenService tokenService, StudentPortalService portalService) {
        this.tokenService = tokenService;
        this.portalService = portalService;
    }

    @GetMapping
    ResponseEntity<?> tuition(
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        try {
            UUID studentId = requireStudentId(authorization);
            return ResponseEntity.ok(ApiResponse.ok(portalService.getStudentTuition(studentId)));
        } catch (ApiException exception) {
            return ResponseEntity.status(exception.getStatus()).body(tuitionLoadFailed());
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(tuitionLoadFailed());
        }
    }

    private UUID requireStudentId(String authorization) {
        CurrentUser currentUser = tokenService.requireUser(authorization);
        if (currentUser.role() != UserRole.STUDENT) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Student tuition is only available for students");
        }
        return currentUser.id();
    }

    private ApiErrorResponse tuitionLoadFailed() {
        return new ApiErrorResponse(false, TUITION_LOAD_FAILED_MESSAGE, null, null);
    }
}
