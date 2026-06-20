package com.fschool.edu.fschool_backend.presentation.controller;

import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.GradeItemResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.SubjectGradesResponse;
import com.fschool.edu.fschool_backend.infrastructure.security.TokenService;
import com.fschool.edu.fschool_backend.application.service.StudentPortalService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/grades")
public class GradeController {

    private final TokenService tokenService;
    private final StudentPortalService portalService;

    public GradeController(TokenService tokenService, StudentPortalService portalService) {
        this.tokenService = tokenService;
        this.portalService = portalService;
    }

    @GetMapping
    ApiResponse<List<SubjectGradesResponse>> grades(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestParam(required = false) UUID semesterId,
            @RequestParam(required = false) UUID subjectId) {
        UUID resolvedSemesterId = semesterId == null ? portalService.getCurrentSemester().id() : semesterId;
        return ApiResponse.ok(portalService.getGrades(tokenService.requireUser(authorization).id(), resolvedSemesterId, subjectId));
    }

    @GetMapping("/recent")
    ApiResponse<List<GradeItemResponse>> recent(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.ok(portalService.getRecentGrades(tokenService.requireUser(authorization).id(), limit));
    }

    @GetMapping("/summary")
    ApiResponse<List<SubjectGradesResponse>> summary(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestParam(required = false) UUID semesterId) {
        UUID resolvedSemesterId = semesterId == null ? portalService.getCurrentSemester().id() : semesterId;
        return ApiResponse.ok(portalService.getGrades(tokenService.requireUser(authorization).id(), resolvedSemesterId, null));
    }

    @GetMapping("/{id}")
    ApiResponse<GradeItemResponse> detail(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable UUID id) {
        return ApiResponse.ok(portalService.getGrade(tokenService.requireUser(authorization).id(), id));
    }
}
