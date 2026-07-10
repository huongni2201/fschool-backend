package com.fschool.edu.fschool_backend.presentation.controller;

import com.fschool.edu.fschool_backend.application.service.StudentAttendanceService;
import com.fschool.edu.fschool_backend.domain.enums.UserRole;
import com.fschool.edu.fschool_backend.infrastructure.security.CurrentUser;
import com.fschool.edu.fschool_backend.infrastructure.security.TokenService;
import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentAttendanceResponse;
import com.fschool.edu.fschool_backend.presentation.exception.ApiException;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/students/me", "/students/me"})
public class StudentAttendanceController {

    private final TokenService tokenService;
    private final StudentAttendanceService attendanceService;

    public StudentAttendanceController(TokenService tokenService, StudentAttendanceService attendanceService) {
        this.tokenService = tokenService;
        this.attendanceService = attendanceService;
    }

    @GetMapping("/attendance")
    ApiResponse<StudentAttendanceResponse> attendance(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        UUID studentId = requireStudentId(authorization);
        return ApiResponse.ok(attendanceService.getStudentAttendance(studentId, from, to));
    }

    private UUID requireStudentId(String authorization) {
        CurrentUser currentUser = tokenService.requireUser(authorization);
        if (currentUser.role() != UserRole.STUDENT) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Attendance is only available for students");
        }
        return currentUser.id();
    }
}
