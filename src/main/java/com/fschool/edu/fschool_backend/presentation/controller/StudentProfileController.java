package com.fschool.edu.fschool_backend.presentation.controller;

import com.fschool.edu.fschool_backend.application.service.StudentProfileService;
import com.fschool.edu.fschool_backend.domain.enums.UserRole;
import com.fschool.edu.fschool_backend.infrastructure.security.CurrentUser;
import com.fschool.edu.fschool_backend.infrastructure.security.TokenService;
import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.MessageResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentProfileResponse;
import com.fschool.edu.fschool_backend.presentation.exception.ApiException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/students/me/profile")
public class StudentProfileController {

    private static final Logger log = LoggerFactory.getLogger(StudentProfileController.class);
    private static final String PROFILE_LOAD_FAILED_MESSAGE =
            "Kh\u00F4ng th\u1EC3 t\u1EA3i th\u00F4ng tin c\u00E1 nh\u00E2n";

    private final TokenService tokenService;
    private final StudentProfileService profileService;

    public StudentProfileController(TokenService tokenService, StudentProfileService profileService) {
        this.tokenService = tokenService;
        this.profileService = profileService;
    }

    @GetMapping
    ResponseEntity<?> profile(@RequestHeader(name = "Authorization", required = false) String authorization) {
        try {
            UUID studentId = requireStudentId(authorization);
            StudentProfileResponse profile = profileService.getStudentProfile(studentId);
            return ResponseEntity.ok(ApiResponse.ok(profile));
        } catch (ApiException exception) {
            log.warn("Cannot load student profile: status={}, message={}", exception.getStatus(), exception.getMessage());
            return ResponseEntity.status(exception.getStatus()).body(profileLoadFailed());
        } catch (Exception exception) {
            log.error("Cannot load student profile", exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(profileLoadFailed());
        }
    }

    private UUID requireStudentId(String authorization) {
        CurrentUser currentUser = tokenService.requireUser(authorization);
        if (currentUser.role() != UserRole.STUDENT) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Student profile is only available for students");
        }
        return currentUser.id();
    }

    private MessageResponse profileLoadFailed() {
        return MessageResponse.error(PROFILE_LOAD_FAILED_MESSAGE);
    }
}
