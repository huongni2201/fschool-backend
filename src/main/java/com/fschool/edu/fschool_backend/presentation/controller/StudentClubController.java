package com.fschool.edu.fschool_backend.presentation.controller;

import com.fschool.edu.fschool_backend.application.service.StudentClubService;
import com.fschool.edu.fschool_backend.presentation.dto.request.ClubRegistrationRequest;
import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.ClubRegistrationCancellationResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.ClubRegistrationResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentClubDetailResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentClubListResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/students/me/clubs", "/students/me/clubs"})
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class StudentClubController {

    private final StudentClubService clubService;

    @GetMapping
    ApiResponse<StudentClubListResponse> clubs(
            Authentication authentication) {
        UUID studentId = currentUserId(authentication);
        return ApiResponse.ok(clubService.getStudentClubs(studentId), "OK");
    }

    @GetMapping("/{clubId}")
    ApiResponse<StudentClubDetailResponse> clubDetail(
            Authentication authentication,
            @PathVariable String clubId) {
        UUID studentId = currentUserId(authentication);
        return ApiResponse.ok(clubService.getStudentClub(studentId, clubId), "OK");
    }

    @PostMapping("/{clubId}/registrations")
    ApiResponse<ClubRegistrationResponse> register(
            Authentication authentication,
            @PathVariable String clubId,
            @RequestBody(required = false) ClubRegistrationRequest request) {
        UUID studentId = currentUserId(authentication);
        return ApiResponse.ok(
                clubService.register(studentId, clubId, request),
                "\u0110\u0103ng k\u00fd c\u00e2u l\u1ea1c b\u1ed9 th\u00e0nh c\u00f4ng");
    }

    @DeleteMapping("/{clubId}/registrations")
    ApiResponse<ClubRegistrationCancellationResponse> cancelRegistration(
            Authentication authentication,
            @PathVariable String clubId,
            @RequestBody(required = false) ClubRegistrationRequest request) {
        UUID studentId = currentUserId(authentication);
        return ApiResponse.ok(
                clubService.cancelRegistration(studentId, clubId, request),
                "\u0110\u00e3 h\u1ee7y \u0111\u0103ng k\u00fd c\u00e2u l\u1ea1c b\u1ed9");
    }

    private UUID currentUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
