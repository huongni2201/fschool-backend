package com.fschool.edu.fschool_backend.presentation.controller;

import com.fschool.edu.fschool_backend.application.service.AdminStudentService;
import com.fschool.edu.fschool_backend.presentation.dto.response.AdminStudentResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/students")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminStudentController {

    private final AdminStudentService studentService;

    @GetMapping
    ApiResponse<PageResponse<AdminStudentResponse>> students(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(studentService.getStudents(page, size, search, classId, status));
    }
}
