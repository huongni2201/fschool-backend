package com.fschool.edu.fschool_backend.presentation.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fschool.edu.fschool_backend.application.service.ParentPortalService;
import com.fschool.edu.fschool_backend.application.service.StudentPortalService;
import com.fschool.edu.fschool_backend.application.service.StudentRequestService;
import com.fschool.edu.fschool_backend.presentation.dto.request.CreateStudentRequestRequest;
import com.fschool.edu.fschool_backend.presentation.dto.response.AcademicPeriodResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.CreateStudentRequestDataResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.CreateStudentRequestResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.GradeSubjectDetailResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.GradeSummaryResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.RequestTypeResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentExamScheduleResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentRequestListResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentRequestTypesResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentTimetableResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentTuitionResponse;
import com.fschool.edu.fschool_backend.presentation.exception.ApiException;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping({"/api/v1/parents/me/students/{studentId}", "/parents/me/students/{studentId}"})
@PreAuthorize("hasRole('PARENT')")
@RequiredArgsConstructor
public class ParentStudentController {

  private final ParentPortalService parentPortalService;
  private final StudentPortalService studentPortalService;
  private final StudentRequestService studentRequestService;
  private final ObjectMapper objectMapper;

  @GetMapping("/timetable")
  ApiResponse<StudentTimetableResponse> timetable(
      Authentication authentication,
      @PathVariable String studentId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    UUID resolvedStudentId = requireLinkedStudentId(authentication, studentId);
    return ApiResponse.ok(studentPortalService.getStudentTimetable(resolvedStudentId, startDate, endDate), "OK");
  }

  @GetMapping("/academic-periods")
  ApiResponse<List<AcademicPeriodResponse>> academicPeriods(
      Authentication authentication,
      @PathVariable String studentId) {
    UUID resolvedStudentId = requireLinkedStudentId(authentication, studentId);
    return ApiResponse.ok(studentPortalService.getAcademicPeriods(resolvedStudentId));
  }

  @GetMapping("/grades/summary")
  ApiResponse<GradeSummaryResponse> gradeSummary(
      Authentication authentication,
      @PathVariable String studentId,
      @RequestParam(required = false) String periodId) {
    UUID resolvedStudentId = requireLinkedStudentId(authentication, studentId);
    return ApiResponse.ok(studentPortalService.getGradeSummary(resolvedStudentId, periodId));
  }

  @GetMapping("/grades/subjects/{subjectId}")
  ApiResponse<GradeSubjectDetailResponse> gradeSubjectDetail(
      Authentication authentication,
      @PathVariable String studentId,
      @PathVariable String subjectId,
      @RequestParam(required = false) String periodId) {
    UUID resolvedStudentId = requireLinkedStudentId(authentication, studentId);
    return ApiResponse.ok(studentPortalService.getGradeSubjectDetail(resolvedStudentId, subjectId, periodId));
  }

  @GetMapping("/exams")
  ApiResponse<StudentExamScheduleResponse> exams(
      Authentication authentication,
      @PathVariable String studentId) {
    UUID resolvedStudentId = requireLinkedStudentId(authentication, studentId);
    return ApiResponse.ok(studentPortalService.getStudentExamSchedule(resolvedStudentId), "OK");
  }

  @GetMapping("/tuition")
  ApiResponse<StudentTuitionResponse> tuition(
      Authentication authentication,
      @PathVariable String studentId) {
    UUID resolvedStudentId = requireLinkedStudentId(authentication, studentId);
    return ApiResponse.ok(studentPortalService.getStudentTuition(resolvedStudentId));
  }

  @GetMapping("/request-types")
  ApiResponse<StudentRequestTypesResponse> requestTypes(
      Authentication authentication,
      @PathVariable String studentId) {
    requireLinkedStudentId(authentication, studentId);
    List<RequestTypeResponse> requestTypes = studentRequestService.getRequestTypes();
    return ApiResponse.ok(new StudentRequestTypesResponse(requestTypes), "OK");
  }

  @GetMapping("/requests")
  ApiResponse<StudentRequestListResponse> requests(
      Authentication authentication,
      @PathVariable String studentId,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String typeCode,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
    UUID resolvedStudentId = requireLinkedStudentId(authentication, studentId);
    return ApiResponse.ok(
        studentRequestService.getStudentRequests(
            resolvedStudentId,
            page,
            limit,
            status,
            typeCode,
            fromDate,
            toDate),
        "OK");
  }

  @PostMapping(value = "/requests", consumes = MediaType.APPLICATION_JSON_VALUE)
  ApiResponse<CreateStudentRequestDataResponse> createJsonRequest(
      Authentication authentication,
      @PathVariable String studentId,
      @RequestBody(required = false) CreateStudentRequestRequest request) {
    UUID resolvedStudentId = requireLinkedStudentId(authentication, studentId);
    CreateStudentRequestResponse response = studentRequestService.createStudentRequest(resolvedStudentId, request);
    return ApiResponse.ok(
        new CreateStudentRequestDataResponse(response),
        "Request submitted successfully");
  }

  @PostMapping(value = "/requests", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ApiResponse<CreateStudentRequestDataResponse> createMultipartRequest(
      Authentication authentication,
      @PathVariable String studentId,
      @RequestParam(required = false) String requestTypeCode,
      @RequestParam(required = false) String title,
      @RequestParam(required = false) String content,
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate,
      @RequestParam(required = false) String fields,
      @RequestPart(name = "attachments", required = false) List<MultipartFile> attachments) {
    UUID resolvedStudentId = requireLinkedStudentId(authentication, studentId);
    CreateStudentRequestRequest request = new CreateStudentRequestRequest(
        requestTypeCode,
        title,
        content,
        startDate,
        endDate,
        parseFields(fields),
        List.of());
    CreateStudentRequestResponse response = studentRequestService.createStudentRequest(
        resolvedStudentId,
        request,
        attachments == null ? List.of() : attachments);
    return ApiResponse.ok(
        new CreateStudentRequestDataResponse(response),
        "Request submitted successfully");
  }

  private UUID requireLinkedStudentId(Authentication authentication, String studentId) {
    return parentPortalService.requireLinkedStudentId(currentUserId(authentication), studentId);
  }

  private UUID currentUserId(Authentication authentication) {
    return UUID.fromString(authentication.getName());
  }

  private Map<String, Object> parseFields(String fields) {
    if (fields == null || fields.isBlank()) {
      return Map.of();
    }
    try {
      return objectMapper.readValue(fields, new TypeReference<>() {
      });
    } catch (IOException exception) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "fields must be a valid JSON object");
    }
  }
}
