package com.fschool.edu.fschool_backend.presentation.controller;

import com.fschool.edu.fschool_backend.application.service.StudentProfileService;
import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.MessageResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentProfileResponse;
import com.fschool.edu.fschool_backend.presentation.exception.ApiException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/students/me/profile")
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class StudentProfileController {

    private static final Logger log = LoggerFactory.getLogger(StudentProfileController.class);
    private static final String PROFILE_LOAD_FAILED_MESSAGE =
            "Kh\u00F4ng th\u1EC3 t\u1EA3i th\u00F4ng tin c\u00E1 nh\u00E2n";

    private final StudentProfileService profileService;

    @GetMapping
    ResponseEntity<?> profile(Authentication authentication) {
        try {
            UUID studentId = currentUserId(authentication);
            StudentProfileResponse profile = profileService.getStudentProfile(studentId);
            return ResponseEntity.ok(ApiResponse.ok(profile));
        } catch (ApiException exception) {
            log.warn("Cannot load student profile: status={}, message={}", exception.getStatus(), exception.getMessage());
            return ResponseEntity.status(exception.getStatus()).body(profileLoadFailed());
        } catch (Exception exception) {
            log.error("Cannot load student profile", exception);
            return ResponseEntity.internalServerError().body(profileLoadFailed());
        }
    }

    private MessageResponse profileLoadFailed() {
        return MessageResponse.error(PROFILE_LOAD_FAILED_MESSAGE);
    }

    private UUID currentUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
