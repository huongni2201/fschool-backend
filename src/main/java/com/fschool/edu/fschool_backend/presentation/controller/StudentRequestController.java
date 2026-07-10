package com.fschool.edu.fschool_backend.presentation.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fschool.edu.fschool_backend.application.service.StudentRequestService;
import com.fschool.edu.fschool_backend.domain.enums.UserRole;
import com.fschool.edu.fschool_backend.infrastructure.security.CurrentUser;
import com.fschool.edu.fschool_backend.infrastructure.security.TokenService;
import com.fschool.edu.fschool_backend.presentation.dto.request.CreateStudentRequestRequest;
import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.CreateStudentRequestDataResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.CreateStudentRequestResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.RequestTypeResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentRequestDetailResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentRequestListResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentRequestTypesResponse;
import com.fschool.edu.fschool_backend.presentation.exception.ApiException;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping({"/api/v1/students/me", "/students/me"})
public class StudentRequestController {

    private final TokenService tokenService;
    private final StudentRequestService requestService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StudentRequestController(
            TokenService tokenService,
            StudentRequestService requestService) {
        this.tokenService = tokenService;
        this.requestService = requestService;
    }

    @GetMapping("/request-types")
    ApiResponse<StudentRequestTypesResponse> requestTypes(
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        requireStudentId(authorization);
        List<RequestTypeResponse> requestTypes = requestService.getRequestTypes();
        return ApiResponse.ok(new StudentRequestTypesResponse(requestTypes), "OK");
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

    @PostMapping(value = "/requests", consumes = MediaType.APPLICATION_JSON_VALUE)
    ApiResponse<CreateStudentRequestDataResponse> createJsonRequest(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestBody(required = false) CreateStudentRequestRequest request) {
        UUID studentId = requireStudentId(authorization);
        CreateStudentRequestResponse response = requestService.createStudentRequest(studentId, request);
        return ApiResponse.ok(
                new CreateStudentRequestDataResponse(response),
                "Request submitted successfully");
    }

    @PostMapping(value = "/requests", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<CreateStudentRequestDataResponse> createMultipartRequest(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestParam(required = false) String requestTypeCode,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String fields,
            @RequestPart(name = "attachments", required = false) List<MultipartFile> attachments) {
        UUID studentId = requireStudentId(authorization);
        CreateStudentRequestRequest request = new CreateStudentRequestRequest(
                requestTypeCode,
                title,
                content,
                startDate,
                endDate,
                parseFields(fields),
                List.of());
        CreateStudentRequestResponse response = requestService.createStudentRequest(
                studentId,
                request,
                attachments == null ? List.of() : attachments);
        return ApiResponse.ok(
                new CreateStudentRequestDataResponse(response),
                "Request submitted successfully");
    }

    private Map<String, Object> parseFields(String fields) {
        if (fields == null || fields.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(fields, new TypeReference<>() {});
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "fields must be a valid JSON object");
        }
    }

    private UUID requireStudentId(String authorization) {
        CurrentUser currentUser = tokenService.requireUser(authorization);
        if (currentUser.role() != UserRole.STUDENT) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Student requests are only available for students");
        }
        return currentUser.id();
    }
}
