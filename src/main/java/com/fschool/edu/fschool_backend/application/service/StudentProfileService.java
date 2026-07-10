package com.fschool.edu.fschool_backend.application.service;

import com.fschool.edu.fschool_backend.domain.enums.Gender;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.ClassEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.SchoolYearEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.UserEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.ClassJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.SchoolYearJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.UserJpaRepository;
import com.fschool.edu.fschool_backend.presentation.dto.response.NotificationSettingsResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentProfileResponse;
import com.fschool.edu.fschool_backend.presentation.exception.ApiException;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
    private final Map<UUID, NotificationSettingsResponse> notificationSettings = new ConcurrentHashMap<>();

    public StudentProfileService(
            UserJpaRepository userRepository,
            ClassJpaRepository classRepository,
            SchoolYearJpaRepository schoolYearRepository,
            @Value("${app.school.campus-name:FPT Schools C\u1EA7u Gi\u1EA5y}") String campusName) {
        this.userRepository = userRepository;
        this.classRepository = classRepository;
        this.schoolYearRepository = schoolYearRepository;
        this.campusName = campusName;
    }

    @Transactional(readOnly = true)
    public StudentProfileResponse getStudentProfile(UUID studentId) {
        UserEntity student = requireStudent(studentId);
        Optional<ClassEntity> clazz = findClass(student.getClassId());
        Optional<SchoolYearEntity> schoolYear = clazz
                .flatMap(value -> schoolYearRepository.findById(value.getSchoolYearId()))
                .or(schoolYearRepository::findByCurrentTrue);

        return new StudentProfileResponse(
                new StudentProfileResponse.Student(
                        student.getFullName(),
                        student.getStudentCode(),
                        avatarText(student.getFullName()),
                        clazz.map(ClassEntity::getName).orElse(null),
                        campusName,
                        schoolYear.map(this::schoolYearLabel).orElse(null),
                        student.getPhone(),
                        null,
                        student.getDateOfBirth(),
                        genderLabel(student.getGender()),
                        student.getAddress()),
                parents(student));
    }

    @Transactional(readOnly = true)
    public NotificationSettingsResponse getNotificationSettings(UUID studentId) {
        requireStudent(studentId);
        return notificationSettings.getOrDefault(studentId, NotificationSettingsResponse.defaults());
    }

    @Transactional(readOnly = true)
    public NotificationSettingsResponse saveNotificationSettings(
            UUID studentId,
            NotificationSettingsResponse settings) {
        requireStudent(studentId);
        notificationSettings.put(studentId, settings);
        return settings;
    }

    private UserEntity requireStudent(UUID studentId) {
        return userRepository.findById(studentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Student was not found"));
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

    private List<StudentProfileResponse.Parent> parents(UserEntity student) {
        if (!hasText(student.getGuardianName()) && !hasText(student.getGuardianPhone())) {
            return List.of();
        }
        return List.of(new StudentProfileResponse.Parent(
                student.getGuardianName(),
                "Ph\u1EE5 huynh",
                student.getGuardianPhone(),
                null,
                student.getAddress()));
    }

    private String genderLabel(Gender gender) {
        if (gender == null) {
            return null;
        }
        return switch (gender) {
            case MALE -> "Nam";
            case FEMALE -> "N\u1EEF";
            case OTHER -> "Kh\u00E1c";
        };
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
                .replace('\u0110', 'D')
                .replace('\u0111', 'd')
                .toUpperCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
