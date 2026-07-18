package com.fschool.edu.fschool_backend.application.service;

import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.TeacherProfileEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.UserEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.TeacherProfileJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.UserJpaRepository;
import com.fschool.edu.fschool_backend.presentation.dto.response.AdminTeacherResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.PageResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.TeacherDepartmentResponse;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminTeacherService {

    private final TeacherProfileJpaRepository teacherProfileRepository;
    private final UserJpaRepository userRepository;

    public AdminTeacherService(
            TeacherProfileJpaRepository teacherProfileRepository,
            UserJpaRepository userRepository) {
        this.teacherProfileRepository = teacherProfileRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminTeacherResponse> getTeachers(
            int page,
            int size,
            String search,
            String departmentId,
            String status) {
        int resolvedPage = Math.max(page, 0);
        int resolvedSize = Math.clamp(size, 1, 100);
        String normalizedSearch = normalizeSearch(search);
        String normalizedDepartmentId = normalizeId(departmentId);
        String normalizedStatus = normalizeStatus(status);

        List<TeacherProfileEntity> teachers = teacherProfileRepository.findAll();
        Map<UUID, UserEntity> usersById = userRepository.findAllById(teachers.stream()
                        .map(TeacherProfileEntity::getUserId)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));

        List<AdminTeacherResponse> filtered = teachers.stream()
                .map(teacher -> toTeacherResponse(teacher, usersById.get(teacher.getUserId())))
                .filter(teacher -> matchesSearch(teacher, normalizedSearch))
                .filter(teacher -> matchesDepartment(teacher, normalizedDepartmentId))
                .filter(teacher -> matchesStatus(teacher, normalizedStatus))
                .sorted(Comparator
                        .comparing(AdminTeacherResponse::fullName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(AdminTeacherResponse::teacherCode, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        int fromIndex = Math.min(resolvedPage * resolvedSize, filtered.size());
        int toIndex = Math.min(fromIndex + resolvedSize, filtered.size());
        return new PageResponse<>(
                filtered.subList(fromIndex, toIndex),
                resolvedPage,
                resolvedSize,
                filtered.size(),
                (int) Math.ceil((double) filtered.size() / resolvedSize));
    }

    @Transactional(readOnly = true)
    public List<TeacherDepartmentResponse> getDepartments() {
        Map<String, TeacherDepartmentResponse> departments = new LinkedHashMap<>();
        teacherProfileRepository.findAll().stream()
                .map(TeacherProfileEntity::getDepartmentName)
                .filter(this::hasText)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(departmentName -> departments.putIfAbsent(
                        normalizeId(departmentName),
                        new TeacherDepartmentResponse(normalizeId(departmentName), departmentName)));
        return List.copyOf(departments.values());
    }

    private AdminTeacherResponse toTeacherResponse(TeacherProfileEntity teacher, UserEntity user) {
        return new AdminTeacherResponse(
                teacher.getUserId(),
                teacher.getEmployeeCode(),
                teacher.getFullName(),
                normalizeId(teacher.getDepartmentName()),
                teacher.getDepartmentName(),
                null,
                user == null ? null : user.getPhone(),
                user == null || user.getStatus() == null ? null : user.getStatus().name());
    }

    private boolean matchesSearch(AdminTeacherResponse teacher, String search) {
        if (!hasText(search)) {
            return true;
        }
        return contains(teacher.fullName(), search)
                || contains(teacher.teacherCode(), search)
                || contains(teacher.phoneNumber(), search);
    }

    private boolean matchesDepartment(AdminTeacherResponse teacher, String departmentId) {
        return !hasText(departmentId) || Objects.equals(teacher.departmentId(), departmentId);
    }

    private boolean matchesStatus(AdminTeacherResponse teacher, String status) {
        return !hasText(status) || Objects.equals(normalizeStatus(teacher.status()), status);
    }

    private boolean contains(String value, String search) {
        return hasText(value) && normalizeSearch(value).contains(search);
    }

    private String normalizeSearch(String value) {
        return normalizeText(value).toLowerCase(Locale.ROOT);
    }

    private String normalizeStatus(String value) {
        return normalizeText(value).toUpperCase(Locale.ROOT);
    }

    private String normalizeId(String value) {
        String normalized = Normalizer.normalize(normalizeText(value).toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
