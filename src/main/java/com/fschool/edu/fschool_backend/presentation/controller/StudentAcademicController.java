package com.fschool.edu.fschool_backend.presentation.controller;

import com.fschool.edu.fschool_backend.application.service.StudentPortalService;
import com.fschool.edu.fschool_backend.presentation.dto.response.AcademicPeriodResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.GradeSubjectDetailResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.GradeSummaryResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/students")
public class StudentAcademicController {

    private final StudentPortalService portalService;

    public StudentAcademicController(StudentPortalService portalService) {
        this.portalService = portalService;
    }

    @GetMapping("/{studentId}/academic-periods")
    ApiResponse<List<AcademicPeriodResponse>> academicPeriods(@PathVariable UUID studentId) {
        return ApiResponse.ok(portalService.getAcademicPeriods(studentId));
    }

    @GetMapping("/{studentId}/grades/summary")
    ApiResponse<GradeSummaryResponse> gradeSummary(
            @PathVariable UUID studentId,
            @RequestParam(required = false) String periodId) {
        return ApiResponse.ok(portalService.getGradeSummary(studentId, periodId));
    }

    @GetMapping("/{studentId}/grades/subjects/{subjectId}")
    ApiResponse<GradeSubjectDetailResponse> gradeSubjectDetail(
            @PathVariable UUID studentId,
            @PathVariable String subjectId,
            @RequestParam(required = false) String periodId) {
        return ApiResponse.ok(portalService.getGradeSubjectDetail(studentId, subjectId, periodId));
    }
}
