package com.fschool.edu.fschool_backend.presentation.controller;

import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.SubjectResponse;
import com.fschool.edu.fschool_backend.application.service.StudentPortalService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subjects")
public class SubjectController {

    private final StudentPortalService portalService;

    public SubjectController(StudentPortalService portalService) {
        this.portalService = portalService;
    }

    @GetMapping
    ApiResponse<List<SubjectResponse>> subjects() {
        return ApiResponse.ok(portalService.getSubjects());
    }

    @GetMapping("/{id}")
    ApiResponse<SubjectResponse> detail(@PathVariable UUID id) {
        return ApiResponse.ok(portalService.getSubject(id));
    }
}
