package com.fschool.edu.fschool_backend.presentation.controller;

import com.fschool.edu.fschool_backend.application.service.StudentClubService;
import com.fschool.edu.fschool_backend.domain.enums.UserRole;
import com.fschool.edu.fschool_backend.infrastructure.security.CurrentUser;
import com.fschool.edu.fschool_backend.infrastructure.security.TokenService;
import com.fschool.edu.fschool_backend.presentation.dto.request.ClubRegistrationRequest;
import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.ClubRegistrationCancellationResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.ClubRegistrationResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentClubDetailResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentClubListResponse;
import com.fschool.edu.fschool_backend.presentation.exception.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/students/me/clubs", "/students/me/clubs"})
public class StudentClubController {

    private final TokenService tokenService;
    private final StudentClubService clubService;

    public StudentClubController(TokenService tokenService, StudentClubService clubService) {
        this.tokenService = tokenService;
        this.clubService = clubService;
    }

    @GetMapping
    ApiResponse<StudentClubListResponse> clubs(
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        UUID studentId = requireStudentId(authorization);
        return ApiResponse.ok(clubService.getStudentClubs(studentId), "OK");
    }

    @GetMapping("/{clubId}")
    ApiResponse<StudentClubDetailResponse> clubDetail(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable String clubId) {
        UUID studentId = requireStudentId(authorization);
        return ApiResponse.ok(clubService.getStudentClub(studentId, clubId), "OK");
    }

    @PostMapping("/{clubId}/registrations")
    ApiResponse<ClubRegistrationResponse> register(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable String clubId,
            @RequestBody(required = false) ClubRegistrationRequest request) {
        UUID studentId = requireStudentId(authorization);
        return ApiResponse.ok(
                clubService.register(studentId, clubId, request),
                "\u0110\u0103ng k\u00fd c\u00e2u l\u1ea1c b\u1ed9 th\u00e0nh c\u00f4ng");
    }

    @DeleteMapping("/{clubId}/registrations")
    ApiResponse<ClubRegistrationCancellationResponse> cancelRegistration(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable String clubId,
            @RequestBody(required = false) ClubRegistrationRequest request) {
        UUID studentId = requireStudentId(authorization);
        return ApiResponse.ok(
                clubService.cancelRegistration(studentId, clubId, request),
                "\u0110\u00e3 h\u1ee7y \u0111\u0103ng k\u00fd c\u00e2u l\u1ea1c b\u1ed9");
    }

    private UUID requireStudentId(String authorization) {
        CurrentUser currentUser = tokenService.requireUser(authorization);
        if (currentUser.role() != UserRole.STUDENT) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Student club APIs are only available for students");
        }
        return currentUser.id();
    }
}
