package com.fschool.edu.fschool_backend.presentation.controller;

import com.fschool.edu.fschool_backend.application.service.StudentRequestService;
import com.fschool.edu.fschool_backend.domain.enums.UserRole;
import com.fschool.edu.fschool_backend.infrastructure.security.CurrentUser;
import com.fschool.edu.fschool_backend.infrastructure.security.TokenService;
import com.fschool.edu.fschool_backend.presentation.dto.request.CreateStudentRequestRequest;
import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.CreateStudentRequestResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.RequestTypeResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentRequestDetailResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentRequestListResponse;
import com.fschool.edu.fschool_backend.presentation.exception.ApiException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/students/me")
public class StudentRequestController {

    private final TokenService tokenService;
    private final StudentRequestService requestService;

    public StudentRequestController(TokenService tokenService, StudentRequestService requestService) {
        this.tokenService = tokenService;
        this.requestService = requestService;
    }

    @GetMapping("/request-types")
    ApiResponse<List<RequestTypeResponse>> requestTypes(
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        requireStudentId(authorization);
        return ApiResponse.ok(requestService.getRequestTypes(), "OK");
    }

    @GetMapping("/requests")
    ApiResponse<StudentRequestListResponse> requests(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String typeCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        UUID studentId = requireStudentId(authorization);
        return ApiResponse.ok(
                requestService.getStudentRequests(studentId, page, limit, status, typeCode, fromDate, toDate),
                "OK");
    }

    @GetMapping("/requests/{requestId}")
    ApiResponse<StudentRequestDetailResponse> requestDetail(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable String requestId) {
        UUID studentId = requireStudentId(authorization);
        return ApiResponse.ok(requestService.getStudentRequest(studentId, requestId), "OK");
    }

    @PostMapping("/requests")
    ApiResponse<CreateStudentRequestResponse> createRequest(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestBody(required = false) CreateStudentRequestRequest request) {
        UUID studentId = requireStudentId(authorization);
        return ApiResponse.ok(requestService.createStudentRequest(studentId, request), "Gửi đơn thành công");
    }

    private UUID requireStudentId(String authorization) {
        CurrentUser currentUser = tokenService.requireUser(authorization);
        if (currentUser.role() != UserRole.STUDENT) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Chỉ học sinh được sử dụng API đơn từ");
        }
        return currentUser.id();
    }
}
