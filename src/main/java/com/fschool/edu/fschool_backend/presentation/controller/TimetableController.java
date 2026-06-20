package com.fschool.edu.fschool_backend.presentation.controller;

import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.LessonResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.TimetableDayResponse;
import com.fschool.edu.fschool_backend.infrastructure.security.TokenService;
import com.fschool.edu.fschool_backend.application.service.StudentPortalService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/timetable")
public class TimetableController {

    private final TokenService tokenService;
    private final StudentPortalService portalService;

    public TimetableController(TokenService tokenService, StudentPortalService portalService) {
        this.tokenService = tokenService;
        this.portalService = portalService;
    }

    @GetMapping("/today")
    ApiResponse<TimetableDayResponse> today(
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        return ApiResponse.ok(portalService.getTimetableForDate(tokenService.requireUser(authorization).id(), LocalDate.now()));
    }

    @GetMapping("/date")
    ApiResponse<TimetableDayResponse> date(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(portalService.getTimetableForDate(tokenService.requireUser(authorization).id(), date));
    }

    @GetMapping("/week")
    ApiResponse<List<TimetableDayResponse>> week(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
        return ApiResponse.ok(portalService.getTimetableWeek(tokenService.requireUser(authorization).id(), startDate));
    }

    @GetMapping("/{id}")
    ApiResponse<LessonResponse> detail(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable UUID id) {
        return ApiResponse.ok(portalService.getLesson(tokenService.requireUser(authorization).id(), id));
    }
}
