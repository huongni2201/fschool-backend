package com.fschool.edu.fschool_backend.presentation.controller;

import com.fschool.edu.fschool_backend.application.service.StudentPortalService;
import com.fschool.edu.fschool_backend.presentation.dto.response.AcademicPeriodResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.GradeSubjectDetailResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.GradeSummaryResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/students"})
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class StudentAcademicController {

    private static final Logger log = LoggerFactory.getLogger(StudentAcademicController.class);

    private final StudentPortalService portalService;

    @GetMapping("/me/academic-periods")
    ApiResponse<List<AcademicPeriodResponse>> myAcademicPeriods(
            Authentication authentication) {
        UUID studentId = currentUserId(authentication);
        log.info("Fetching academic periods for current student: studentId={}", studentId);
        return ApiResponse.ok(portalService.getAcademicPeriods(studentId));
    }

    @GetMapping("/me/grades/summary")
    ApiResponse<GradeSummaryResponse> myGradeSummary(
            Authentication authentication,
            @RequestParam(required = false) String periodId) {
        UUID studentId = currentUserId(authentication);
        log.info("Fetching grade summary for current student: studentId={}, periodId={}", studentId, periodId);
        return ApiResponse.ok(portalService.getGradeSummary(studentId, periodId));
    }

    @GetMapping("/me/grades/subjects/{subjectId}")
    ApiResponse<GradeSubjectDetailResponse> myGradeSubjectDetail(
            Authentication authentication,
            @PathVariable String subjectId,
            @RequestParam(required = false) String periodId) {
        UUID studentId = currentUserId(authentication);
        log.info(
                "Fetching grade subject detail for current student: studentId={}, subjectId={}, periodId={}",
                studentId,
                subjectId,
                periodId);
        return ApiResponse.ok(portalService.getGradeSubjectDetail(studentId, subjectId, periodId));
    }

    private UUID currentUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
