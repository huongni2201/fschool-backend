package com.fschool.edu.fschool_backend.presentation.controller;

import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.CurrentSemesterResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.SchoolYearResponse;
import com.fschool.edu.fschool_backend.application.service.StudentPortalService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/academic")
public class AcademicController {

    private final StudentPortalService portalService;

    public AcademicController(StudentPortalService portalService) {
        this.portalService = portalService;
    }

    @GetMapping("/current-school-year")
    ApiResponse<SchoolYearResponse> currentSchoolYear() {
        return ApiResponse.ok(portalService.getCurrentSchoolYear());
    }

    @GetMapping("/current-semester")
    ApiResponse<CurrentSemesterResponse> currentSemester() {
        return ApiResponse.ok(portalService.getCurrentSemester());
    }

    @GetMapping("/semesters")
    ApiResponse<List<CurrentSemesterResponse>> semesters() {
        return ApiResponse.ok(portalService.getSemesters());
    }
}
