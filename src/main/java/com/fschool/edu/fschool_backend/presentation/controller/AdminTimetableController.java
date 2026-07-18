package com.fschool.edu.fschool_backend.presentation.controller;

import com.fschool.edu.fschool_backend.application.service.AdminClassService;
import com.fschool.edu.fschool_backend.application.service.AdminTimetableService;
import com.fschool.edu.fschool_backend.presentation.dto.response.AcademicYearFilterResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.AdminTimetableResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.GradeLevelFilterResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.SemesterFilterResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.TimetableImportResponse;
import com.fschool.edu.fschool_backend.presentation.exception.ApiException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'HOMEROOM_TEACHER')")
@RequiredArgsConstructor
public class AdminTimetableController {

    private final AdminTimetableService timetableService;
    private final AdminClassService classService;

    @GetMapping("/timetables")
    ApiResponse<AdminTimetableResponse> timetables(
            @RequestParam(required = false) UUID academicYearId,
            @RequestParam(required = false) UUID semesterId,
            @RequestParam(required = false) Integer gradeLevel,
            @RequestParam(required = false) UUID classId,
            Authentication authentication) {
        return ApiResponse.ok(timetableService.getTimetable(
                academicYearId,
                semesterId,
                gradeLevel,
                classId,
                currentUserId(authentication),
                isAdmin(authentication)));
    }

    @PostMapping(value = "/timetables/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<TimetableImportResponse> importTimetable(
            @RequestPart("file") MultipartFile file,
            @RequestParam UUID academicYearId,
            @RequestParam UUID semesterId,
            @RequestParam UUID classId,
            Authentication authentication) {
        TimetableImportResponse response = timetableService.importTimetable(
                file,
                academicYearId,
                semesterId,
                classId,
                currentUserId(authentication));
        return ApiResponse.ok(response, importMessage(response));
    }

    @GetMapping("/timetables/import-template")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<byte[]> importTemplate() {
        byte[] fileContent = timetableService.exportImportTemplate();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("timetable_import_template.xlsx")
                        .build()
                        .toString())
                .body(fileContent);
    }

    @GetMapping("/academic-years")
    ApiResponse<List<AcademicYearFilterResponse>> academicYears() {
        return ApiResponse.ok(timetableService.getAcademicYears());
    }

    @GetMapping("/semesters")
    ApiResponse<List<SemesterFilterResponse>> semesters(
            @RequestParam(required = false) UUID academicYearId) {
        return ApiResponse.ok(timetableService.getSemesters(academicYearId));
    }

    @GetMapping("/grade-levels")
    ApiResponse<List<GradeLevelFilterResponse>> gradeLevels() {
        return ApiResponse.ok(timetableService.getGradeLevels());
    }

    @GetMapping("/classes")
    ApiResponse<?> classes(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer gradeLevel,
            @RequestParam(required = false) UUID semesterId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String scope,
            Authentication authentication) {
        if (isClassPageRequest(page, size, search, status)) {
            requireAdmin(authentication);
            return ApiResponse.ok(classService.getClasses(
                    page == null ? 0 : page,
                    size == null ? 10 : size,
                    search,
                    gradeLevel,
                    status));
        }
        return ApiResponse.ok(classService.getClassFilters(
                gradeLevel,
                currentUserId(authentication),
                isAdmin(authentication),
                semesterId,
                scope));
    }

    private boolean isClassPageRequest(Integer page, Integer size, String search, String status) {
        return page != null || size != null || hasText(search) || hasText(status);
    }

    private UUID currentUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    private void requireAdmin(Authentication authentication) {
        if (!isAdmin(authentication)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Forbidden");
        }
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null
                && authentication.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private String importMessage(TimetableImportResponse response) {
        if (!response.hasErrors()) {
            return "Import completed successfully";
        }
        return response.successRows() == 0
                ? "Import failed with validation errors"
                : "Import completed with validation errors";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
