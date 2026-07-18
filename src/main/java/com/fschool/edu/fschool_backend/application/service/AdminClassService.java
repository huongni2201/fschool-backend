package com.fschool.edu.fschool_backend.application.service;

import com.fschool.edu.fschool_backend.domain.enums.ClassStatus;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.ClassEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.TeacherProfileEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.ClassJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.TeacherProfileJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.UserJpaRepository;
import com.fschool.edu.fschool_backend.presentation.dto.response.AdminClassResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.ClassFilterResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.PageResponse;
import com.fschool.edu.fschool_backend.presentation.exception.ApiException;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminClassService {

    private final ClassJpaRepository classRepository;
    private final TeacherProfileJpaRepository teacherProfileRepository;
    private final UserJpaRepository userRepository;
    private final TeacherGradeAccessService teacherGradeAccessService;

    public AdminClassService(
            ClassJpaRepository classRepository,
            TeacherProfileJpaRepository teacherProfileRepository,
            UserJpaRepository userRepository,
            TeacherGradeAccessService teacherGradeAccessService) {
        this.classRepository = classRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.userRepository = userRepository;
        this.teacherGradeAccessService = teacherGradeAccessService;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminClassResponse> getClasses(
            int page,
            int size,
            String search,
            Integer gradeLevel,
            String status) {
        int resolvedPage = Math.max(page, 0);
        int resolvedSize = Math.max(1, Math.min(size, 100));
        String normalizedSearch = normalizeSearch(search);
        ClassStatus resolvedStatus = resolveStatus(status);

        List<ClassEntity> classes = classRepository.findAll().stream()
                .filter(schoolClass -> gradeLevel == null || Objects.equals(schoolClass.getGradeNumber(), gradeLevel.shortValue()))
                .filter(schoolClass -> resolvedStatus == null || schoolClass.getStatus() == resolvedStatus)
                .filter(schoolClass -> matchesSearch(schoolClass, normalizedSearch))
                .sorted(Comparator
                        .comparingInt((ClassEntity schoolClass) -> schoolClass.getGradeNumber() == null
                                ? Integer.MAX_VALUE
                                : schoolClass.getGradeNumber())
                        .thenComparing(ClassEntity::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        Map<UUID, TeacherProfileEntity> teachersByUserId = teachersByUserId(classes);
        Map<String, List<TeacherProfileEntity>> teachersByName = teachersByName();
        List<AdminClassResponse> responses = classes.stream()
                .map(schoolClass -> toClassResponse(
                        schoolClass,
                        resolveHomeroomTeacher(schoolClass, teachersByUserId, teachersByName)))
                .toList();

        int fromIndex = Math.min(resolvedPage * resolvedSize, responses.size());
        int toIndex = Math.min(fromIndex + resolvedSize, responses.size());
        return new PageResponse<>(
                responses.subList(fromIndex, toIndex),
                resolvedPage,
                resolvedSize,
                responses.size(),
                (int) Math.ceil((double) responses.size() / resolvedSize));
    }

    @Transactional(readOnly = true)
    public List<ClassFilterResponse> getClassFilters(
            Integer gradeLevel,
            UUID currentUserId,
            boolean admin,
            UUID semesterId,
            String scope) {
        List<ClassEntity> classes;
        if (admin) {
            classes = allClasses(gradeLevel);
        } else if ("timetable".equals(normalizeSearch(scope))) {
            classes = teacherGradeAccessService.teachingClasses(currentUserId, semesterId, gradeLevel);
        } else {
            classes = teacherGradeAccessService.accessibleClasses(currentUserId, gradeLevel);
        }
        return classes.stream()
                .sorted(Comparator
                        .comparingInt((ClassEntity schoolClass) -> schoolClass.getGradeNumber() == null
                                ? Integer.MAX_VALUE
                                : schoolClass.getGradeNumber())
                        .thenComparing(ClassEntity::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(schoolClass -> new ClassFilterResponse(
                        schoolClass.getId(),
                        schoolClass.getName(),
                        schoolClass.getGradeNumber()))
                .toList();
    }

    private AdminClassResponse toClassResponse(ClassEntity schoolClass, TeacherProfileEntity teacher) {
        String classCode = classCode(schoolClass.getName());
        return new AdminClassResponse(
                schoolClass.getId(),
                classCode,
                classDisplayName(classCode, schoolClass.getName()),
                schoolClass.getGradeNumber() == null ? 0 : schoolClass.getGradeNumber(),
                userRepository.countStudentsByClassId(schoolClass.getId()),
                teacher == null ? schoolClass.getHomeroomTeacherId() : teacher.getUserId(),
                teacher == null ? null : teacher.getEmployeeCode(),
                teacher == null ? schoolClass.getHomeroomTeacherName() : teacher.getFullName(),
                schoolClass.getStatus() == null ? null : schoolClass.getStatus().name());
    }

    private List<ClassEntity> allClasses(Integer gradeLevel) {
        return gradeLevel == null
                ? classRepository.findAll()
                : classRepository.findByGradeNumberOrderByNameAsc(gradeLevel.shortValue());
    }

    private Map<UUID, TeacherProfileEntity> teachersByUserId(List<ClassEntity> classes) {
        List<UUID> teacherIds = classes.stream()
                .map(ClassEntity::getHomeroomTeacherId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (teacherIds.isEmpty()) {
            return Map.of();
        }
        return teacherProfileRepository.findByUserIdIn(teacherIds).stream()
                .collect(Collectors.toMap(TeacherProfileEntity::getUserId, Function.identity()));
    }

    private Map<String, List<TeacherProfileEntity>> teachersByName() {
        return teacherProfileRepository.findAll().stream()
                .filter(teacher -> hasText(teacher.getFullName()))
                .collect(Collectors.groupingBy(teacher -> normalizeSearch(teacher.getFullName())));
    }

    private TeacherProfileEntity resolveHomeroomTeacher(
            ClassEntity schoolClass,
            Map<UUID, TeacherProfileEntity> teachersByUserId,
            Map<String, List<TeacherProfileEntity>> teachersByName) {
        if (schoolClass.getHomeroomTeacherId() != null) {
            return teachersByUserId.get(schoolClass.getHomeroomTeacherId());
        }
        if (!hasText(schoolClass.getHomeroomTeacherName())) {
            return null;
        }
        List<TeacherProfileEntity> matchedTeachers = teachersByName.get(normalizeSearch(schoolClass.getHomeroomTeacherName()));
        if (matchedTeachers == null || matchedTeachers.size() != 1) {
            return null;
        }
        return matchedTeachers.getFirst();
    }

    private boolean matchesSearch(ClassEntity schoolClass, String search) {
        if (!hasText(search)) {
            return true;
        }
        String classCode = classCode(schoolClass.getName());
        return contains(classCode, search)
                || contains(classDisplayName(classCode, schoolClass.getName()), search)
                || contains(schoolClass.getHomeroomTeacherName(), search);
    }

    private boolean contains(String value, String search) {
        return hasText(value) && normalizeSearch(value).contains(search);
    }

    private ClassStatus resolveStatus(String status) {
        if (!hasText(status)) {
            return null;
        }
        try {
            return ClassStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "status is invalid");
        }
    }

    private String classCode(String name) {
        if (!hasText(name)) {
            return name;
        }
        String trimmed = name.trim();
        String normalized = normalizeSearch(trimmed);
        if (normalized.startsWith("lop ")) {
            return trimmed.substring(trimmed.indexOf(' ') + 1).trim();
        }
        return trimmed;
    }

    private String classDisplayName(String classCode, String rawName) {
        if (!hasText(rawName)) {
            return rawName;
        }
        String trimmed = rawName.trim();
        if (normalizeSearch(trimmed).startsWith("lop ")) {
            return trimmed;
        }
        return "L\u1edbp " + classCode;
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
