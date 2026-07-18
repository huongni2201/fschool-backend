package com.fschool.edu.fschool_backend.application.service;

import com.fschool.edu.fschool_backend.domain.constants.RoleCodes;
import com.fschool.edu.fschool_backend.domain.enums.UserStatus;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.ClassEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.UserEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.ClassJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.UserJpaRepository;
import com.fschool.edu.fschool_backend.presentation.dto.response.AdminStudentResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.PageResponse;
import com.fschool.edu.fschool_backend.presentation.exception.ApiException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminStudentService {

    private final UserJpaRepository userRepository;
    private final ClassJpaRepository classRepository;

    public AdminStudentService(
            UserJpaRepository userRepository,
            ClassJpaRepository classRepository) {
        this.userRepository = userRepository;
        this.classRepository = classRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminStudentResponse> getStudents(
            int page,
            int size,
            String search,
            String classId,
            String status) {
        int resolvedPage = Math.max(page, 0);
        int resolvedSize = Math.max(1, Math.min(size, 100));
        String normalizedSearch = normalize(search);
        Set<UUID> classIds = resolveClassIds(classId);
        UserStatus resolvedStatus = resolveStatus(status);

        List<UserEntity> students = userRepository.findAll().stream()
                .filter(this::isStudent)
                .filter(student -> classIds == null || classIds.contains(student.getClassId()))
                .filter(student -> resolvedStatus == null || student.getStatus() == resolvedStatus)
                .filter(student -> matchesSearch(student, normalizedSearch))
                .sorted(Comparator
                        .comparing(UserEntity::getFullName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(UserEntity::getStudentCode, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        Map<UUID, ClassEntity> classesById = classRepository.findAllById(students.stream()
                        .map(UserEntity::getClassId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(ClassEntity::getId, Function.identity()));
        List<AdminStudentResponse> responses = students.stream()
                .map(student -> toStudentResponse(student, classesById.get(student.getClassId())))
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

    private AdminStudentResponse toStudentResponse(UserEntity student, ClassEntity schoolClass) {
        return new AdminStudentResponse(
                student.getId(),
                student.getStudentCode(),
                student.getFullName(),
                student.getClassId(),
                schoolClass == null ? null : schoolClass.getName(),
                student.getDateOfBirth(),
                student.getGender(),
                student.getPhone(),
                student.getStatus() == null ? null : student.getStatus().name());
    }

    private boolean isStudent(UserEntity user) {
        return user.getRole() != null && RoleCodes.STUDENT.equals(user.getRole().getCode());
    }

    private boolean matchesSearch(UserEntity student, String search) {
        if (!hasText(search)) {
            return true;
        }
        return contains(student.getFullName(), search)
                || contains(student.getStudentCode(), search)
                || contains(student.getPhone(), search);
    }

    private boolean contains(String value, String search) {
        return hasText(value) && normalize(value).contains(search);
    }

    private Set<UUID> resolveClassIds(String classId) {
        if (!hasText(classId)) {
            return null;
        }
        try {
            return Set.of(UUID.fromString(classId.trim()));
        } catch (IllegalArgumentException exception) {
            List<ClassEntity> classes = classRepository.findByNameIgnoreCase(classId.trim());
            return classes.stream().map(ClassEntity::getId).collect(Collectors.toSet());
        }
    }

    private UserStatus resolveStatus(String status) {
        if (!hasText(status)) {
            return null;
        }
        try {
            return UserStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "status is invalid");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
