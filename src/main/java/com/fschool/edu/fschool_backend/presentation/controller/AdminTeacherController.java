package com.fschool.edu.fschool_backend.presentation.controller;

import com.fschool.edu.fschool_backend.application.service.AdminTeacherService;
import com.fschool.edu.fschool_backend.presentation.dto.response.AdminTeacherResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.PageResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.TeacherDepartmentResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teachers")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminTeacherController {

    private final AdminTeacherService teacherService;

    @GetMapping
    ApiResponse<PageResponse<AdminTeacherResponse>> teachers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(teacherService.getTeachers(page, size, search, departmentId, status));
    }

    @GetMapping("/departments")
    ApiResponse<List<TeacherDepartmentResponse>> departments() {
        return ApiResponse.ok(teacherService.getDepartments());
    }
}
