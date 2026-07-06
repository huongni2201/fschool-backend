package com.fschool.edu.fschool_backend.application.service;

import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.ClassEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.SchoolYearEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.UserEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.ClassJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.SchoolYearJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.UserJpaRepository;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentProfileResponse;
import com.fschool.edu.fschool_backend.presentation.exception.ApiException;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentProfileService {

    private final UserJpaRepository userRepository;
    private final ClassJpaRepository classRepository;
    private final SchoolYearJpaRepository schoolYearRepository;
    private final String campusName;

    public StudentProfileService(
            UserJpaRepository userRepository,
            ClassJpaRepository classRepository,
            SchoolYearJpaRepository schoolYearRepository,
            @Value("${app.school.campus-name:FPT Schools Cầu Giấy}") String campusName) {
        this.userRepository = userRepository;
        this.classRepository = classRepository;
        this.schoolYearRepository = schoolYearRepository;
        this.campusName = campusName;
    }

    @Transactional(readOnly = true)
    public StudentProfileResponse getStudentProfile(UUID studentId) {
        UserEntity student = userRepository.findById(studentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Student was not found"));
        Optional<ClassEntity> clazz = findClass(student.getClassId());
        Optional<SchoolYearEntity> schoolYear = clazz
                .flatMap(value -> schoolYearRepository.findById(value.getSchoolYearId()))
                .or(schoolYearRepository::findByCurrentTrue);

        return new StudentProfileResponse(
                student.getId().toString(),
                student.getStudentCode(),
                student.getFullName(),
                student.getFullName(),
                student.getAvatarUrl(),
                avatarText(student.getFullName()),
                student.getGender() == null ? null : student.getGender().name(),
                student.getDateOfBirth(),
                null,
                student.getPhone(),
                clazz.map(ClassEntity::getName).orElse(null),
                campusName,
                schoolYear.map(this::schoolYearLabel).orElse(null));
    }

    private Optional<ClassEntity> findClass(UUID classId) {
        return classId == null ? Optional.empty() : classRepository.findById(classId);
    }

    private String schoolYearLabel(SchoolYearEntity schoolYear) {
        if (schoolYear.getStartDate() != null && schoolYear.getEndDate() != null) {
            return schoolYear.getStartDate().getYear() + " - " + schoolYear.getEndDate().getYear();
        }
        return schoolYear.getName();
    }

    private String avatarText(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return normalizeInitials(parts[0].substring(0, Math.min(2, parts[0].length())));
        }
        return normalizeInitials(firstChar(parts[0]) + firstChar(parts[parts.length - 1]));
    }

    private String firstChar(String value) {
        return value == null || value.isBlank() ? "" : value.substring(0, 1);
    }

    private String normalizeInitials(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('Đ', 'D')
                .replace('đ', 'd')
                .toUpperCase(Locale.ROOT);
    }
}
