package com.fschool.edu.fschool_backend.presentation.controller;

import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.AssignmentResponse;
import com.fschool.edu.fschool_backend.infrastructure.security.TokenService;
import com.fschool.edu.fschool_backend.application.service.StudentPortalService;
import com.fschool.edu.fschool_backend.domain.enums.AssignmentStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assignments")
public class AssignmentController {

    private final TokenService tokenService;
    private final StudentPortalService portalService;

    public AssignmentController(TokenService tokenService, StudentPortalService portalService) {
        this.tokenService = tokenService;
        this.portalService = portalService;
    }

    @GetMapping
    ApiResponse<List<AssignmentResponse>> assignments(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestParam(required = false) AssignmentStatus status,
            @RequestParam(required = false) UUID subjectId,
            @RequestParam(required = false) UUID semesterId,
            @RequestParam(defaultValue = "false") boolean upcoming) {
        return ApiResponse.ok(portalService.getAssignments(
                tokenService.requireUser(authorization).id(), status, subjectId, semesterId, upcoming));
    }

    @GetMapping("/{id}")
    ApiResponse<AssignmentResponse> detail(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable UUID id) {
        return ApiResponse.ok(portalService.getAssignment(tokenService.requireUser(authorization).id(), id));
    }
}
