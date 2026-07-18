package com.fschool.edu.fschool_backend.presentation.controller;

import com.fschool.edu.fschool_backend.application.service.AdminGradeService;
import com.fschool.edu.fschool_backend.application.service.AdminSubjectService;
import com.fschool.edu.fschool_backend.presentation.dto.response.AdminGradeResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.SubjectFilterResponse;
import com.fschool.edu.fschool_backend.presentation.exception.ApiException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'HOMEROOM_TEACHER')")
@RequiredArgsConstructor
public class AdminGradeController {

    private final AdminGradeService gradeService;
    private final AdminSubjectService subjectService;

    @GetMapping("/subjects")
    ApiResponse<?> subjects(
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) UUID semesterId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer gradeLevel,
            @RequestParam(required = false) String status,
            Authentication authentication) {
        if (isSubjectPageRequest(page, size, search, gradeLevel, status)) {
            requireAdmin(authentication);
            return ApiResponse.ok(subjectService.getSubjects(
                    page == null ? 0 : page,
                    size == null ? 10 : size,
                    search,
                    gradeLevel,
                    status));
        }
        return ApiResponse.ok(gradeService.getSubjects(
                classId,
                semesterId,
                currentUserId(authentication),
                isAdmin(authentication)));
    }

    @GetMapping("/grades")
    ApiResponse<List<AdminGradeResponse>> grades(
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) UUID semesterId,
            @RequestParam(required = false) UUID subjectId,
            @RequestParam(required = false) String search,
            Authentication authentication) {
        return ApiResponse.ok(gradeService.getGrades(
                classId,
                semesterId,
                subjectId,
                search,
                currentUserId(authentication),
                isAdmin(authentication)));
    }

    private boolean isSubjectPageRequest(
            Integer page,
            Integer size,
            String search,
            Integer gradeLevel,
            String status) {
        return page != null
                || size != null
                || hasText(search)
                || gradeLevel != null
                || hasText(status);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
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
}
