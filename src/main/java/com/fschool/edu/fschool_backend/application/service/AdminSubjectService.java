package com.fschool.edu.fschool_backend.application.service;

import com.fschool.edu.fschool_backend.domain.enums.SubjectStatus;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.SubjectEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.SubjectJpaRepository;
import com.fschool.edu.fschool_backend.presentation.dto.response.AdminSubjectResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.PageResponse;
import com.fschool.edu.fschool_backend.presentation.exception.ApiException;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminSubjectService {

    private final SubjectJpaRepository subjectRepository;

    public AdminSubjectService(SubjectJpaRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminSubjectResponse> getSubjects(
            int page,
            int size,
            String search,
            Integer gradeLevel,
            String status) {
        int resolvedPage = Math.max(page, 0);
        int resolvedSize = Math.max(1, Math.min(size, 100));
        String normalizedSearch = normalizeSearch(search);
        SubjectStatus resolvedStatus = resolveStatus(status);

        List<AdminSubjectResponse> subjects = subjectRepository.findAll().stream()
                .filter(subject -> resolvedStatus == null || subject.getStatus() == resolvedStatus)
                .map(this::toSubjectResponse)
                .filter(subject -> gradeLevel == null || subject.gradeLevels().contains(gradeLevel))
                .filter(subject -> matchesSearch(subject, normalizedSearch))
                .sorted(Comparator
                        .comparing(AdminSubjectResponse::name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(AdminSubjectResponse::subjectCode, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        int fromIndex = Math.min(resolvedPage * resolvedSize, subjects.size());
        int toIndex = Math.min(fromIndex + resolvedSize, subjects.size());
        return new PageResponse<>(
                subjects.subList(fromIndex, toIndex),
                resolvedPage,
                resolvedSize,
                subjects.size(),
                (int) Math.ceil((double) subjects.size() / resolvedSize));
    }

    private AdminSubjectResponse toSubjectResponse(SubjectEntity subject) {
        return new AdminSubjectResponse(
                subject.getId(),
                subject.getCode(),
                subject.getName(),
                parseGradeLevels(subject.getGradeLevels()),
                subject.getLessonsPerWeek() == null ? 0 : subject.getLessonsPerWeek(),
                subject.getStatus() == null ? null : subject.getStatus().name());
    }

    private boolean matchesSearch(AdminSubjectResponse subject, String search) {
        if (!hasText(search)) {
            return true;
        }
        return contains(subject.subjectCode(), search)
                || contains(subject.name(), search);
    }

    private boolean contains(String value, String search) {
        return hasText(value) && normalizeSearch(value).contains(search);
    }

    private List<Integer> parseGradeLevels(String gradeLevels) {
        if (!hasText(gradeLevels)) {
            return List.of();
        }
        return List.of(gradeLevels.split(",")).stream()
                .map(String::trim)
                .filter(this::hasText)
                .map(this::parseGradeLevel)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }

    private Integer parseGradeLevel(String gradeLevel) {
        try {
            return Integer.parseInt(gradeLevel);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private SubjectStatus resolveStatus(String status) {
        if (!hasText(status)) {
            return null;
        }
        try {
            return SubjectStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "status is invalid");
        }
    }

    private String normalizeSearch(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.toLowerCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
