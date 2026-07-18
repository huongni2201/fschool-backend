package com.fschool.edu.fschool_backend.presentation.controller;

import com.fschool.edu.fschool_backend.application.service.StudentRequestService;
import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.RequestTypeResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentRequestDetailResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentRequestListResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentRequestTypesResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/students/me", "/students/me"})
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class StudentRequestController {

    private final StudentRequestService requestService;

    @GetMapping("/request-types")
    ApiResponse<StudentRequestTypesResponse> requestTypes() {
        List<RequestTypeResponse> requestTypes = requestService.getRequestTypes();
        return ApiResponse.ok(new StudentRequestTypesResponse(requestTypes), "OK");
    }

    @GetMapping("/requests")
    ApiResponse<StudentRequestListResponse> requests(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String typeCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        UUID studentId = currentUserId(authentication);
        return ApiResponse.ok(
                requestService.getStudentRequests(studentId, page, limit, status, typeCode, fromDate, toDate),
                "OK");
    }

    @GetMapping("/requests/{requestId}")
    ApiResponse<StudentRequestDetailResponse> requestDetail(
            Authentication authentication,
            @PathVariable String requestId) {
        UUID studentId = currentUserId(authentication);
        return ApiResponse.ok(requestService.getStudentRequest(studentId, requestId), "OK");
    }

    private UUID currentUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
