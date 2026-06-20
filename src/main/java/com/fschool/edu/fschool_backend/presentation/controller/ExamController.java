package com.fschool.edu.fschool_backend.presentation.controller;

import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.ExamResponse;
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
@RequestMapping("/api/v1/exams")
public class ExamController {

    private final TokenService tokenService;
    private final StudentPortalService portalService;

    public ExamController(TokenService tokenService, StudentPortalService portalService) {
        this.tokenService = tokenService;
        this.portalService = portalService;
    }

    @GetMapping
    ApiResponse<List<ExamResponse>> exams(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestParam(required = false) UUID semesterId,
            @RequestParam(required = false) UUID subjectId,
            @RequestParam(defaultValue = "false") boolean upcoming) {
        return ApiResponse.ok(portalService.getExams(
                tokenService.requireUser(authorization).id(), semesterId, subjectId, upcoming));
    }

    @GetMapping("/{id}")
    ApiResponse<ExamResponse> detail(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable UUID id) {
        return ApiResponse.ok(portalService.getExam(tokenService.requireUser(authorization).id(), id));
    }
}
