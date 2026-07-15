package com.fschool.edu.fschool_backend.application.service;

import com.fschool.edu.fschool_backend.domain.constants.RoleCodes;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.ClassEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.GradeEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.RoleEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.SemesterEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.SubjectEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.TimetableEntryEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.UserEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.ClassJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.GradeJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.NotificationJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.SchoolYearJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.SemesterJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.SubjectJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.TimetableEntryJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.UserJpaRepository;
import com.fschool.edu.fschool_backend.presentation.dto.response.ParentDashboardResponse;
import com.fschool.edu.fschool_backend.presentation.exception.ApiException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ParentPortalService {

    private static final ZoneId DASHBOARD_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final UserJpaRepository userRepository;
    private final ClassJpaRepository classRepository;
    private final NotificationJpaRepository notificationRepository;
    private final GradeJpaRepository gradeRepository;
    private final SchoolYearJpaRepository schoolYearRepository;
    private final SemesterJpaRepository semesterRepository;
    private final TimetableEntryJpaRepository timetableRepository;
    private final SubjectJpaRepository subjectRepository;

    @Transactional(readOnly = true)
    public ParentDashboardResponse getDashboard(UUID parentId) {
        UserEntity parent = requireParent(parentId);
        long unreadCount = notificationRepository.countByUserIdAndReadFalse(parent.getId());
        List<ParentDashboardResponse.Alert> alerts = unreadCount > 0
                ? List.of(new ParentDashboardResponse.Alert(
                        "meeting",
                        "Thông báo mới",
                        "Nhà trường có lịch họp phụ huynh trong tuần này."))
                : List.of();

        return new ParentDashboardResponse(
                new ParentDashboardResponse.Parent(parent.getId().toString(), parent.getFullName()),
                unreadCount,
                Instant.now(),
                linkedStudent(parent)
                        .map(this::toDashboardStudent)
                        .orElse(null),
                alerts);
    }

    @Transactional(readOnly = true)
    public UUID requireLinkedStudentId(UUID parentId, String studentId) {
        UserEntity parent = requireParent(parentId);
        UserEntity student = resolveStudent(studentId);
        if (!isStudent(student)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Student was not found");
        }
        if (!samePhone(parent.getPhone(), student.getGuardianPhone())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Student is not linked to this parent");
        }
        return student.getId();
    }

    private UserEntity requireParent(UUID parentId) {
        UserEntity parent = userRepository.findById(parentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Parent was not found"));
        if (!RoleCodes.PARENT.equals(roleCode(parent))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Parent APIs are only available for parents");
        }
        return parent;
    }

    private UserEntity resolveStudent(String studentId) {
        if (!hasText(studentId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Student was not found");
        }
        try {
            return userRepository.findById(UUID.fromString(studentId))
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Student was not found"));
        } catch (IllegalArgumentException exception) {
            return userRepository.findByStudentCode(studentId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Student was not found"));
        }
    }

    private Optional<UserEntity> linkedStudent(UserEntity parent) {
        return userRepository.findByGuardianPhoneIn(phoneCandidates(parent.getPhone())).stream()
                .filter(this::isStudent)
                .filter(student -> samePhone(parent.getPhone(), student.getGuardianPhone()))
                .findFirst();
    }

    private ParentDashboardResponse.Student toDashboardStudent(UserEntity student) {
        Optional<ClassEntity> clazz = findClass(student.getClassId());
        return new ParentDashboardResponse.Student(
                student.getId().toString(),
                student.getStudentCode(),
                student.getFullName(),
                clazz.map(ClassEntity::getName).orElse(null),
                avatarText(student.getFullName()),
                gradeAverage(student.getId()),
                new ParentDashboardResponse.Tuition("Đã hoàn tất", 0),
                clazz.flatMap(this::nextLesson).orElse(null),
                "Đang học tại trường",
                clazz.flatMap(this::homeroomTeacher).orElse(null),
                List.of());
    }

    private Optional<ClassEntity> findClass(UUID classId) {
        return classId == null ? Optional.empty() : classRepository.findById(classId);
    }

    private BigDecimal gradeAverage(UUID studentId) {
        List<GradeEntity> grades = gradeRepository.findByUserIdOrderByAssessmentDateDesc(studentId).stream()
                .filter(grade -> grade.getScore() != null)
                .toList();
        if (grades.isEmpty()) {
            return null;
        }
        BigDecimal total = BigDecimal.ZERO;
        int count = 0;
        for (GradeEntity grade : grades) {
            BigDecimal maxScore = grade.getMaxScore() == null || grade.getMaxScore().compareTo(BigDecimal.ZERO) <= 0
                    ? BigDecimal.TEN
                    : grade.getMaxScore();
            total = total.add(grade.getScore()
                    .multiply(BigDecimal.TEN)
                    .divide(maxScore, 2, RoundingMode.HALF_UP));
            count++;
        }
        return total.divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP);
    }

    private Optional<ParentDashboardResponse.NextLesson> nextLesson(ClassEntity clazz) {
        LocalDate today = LocalDate.now(DASHBOARD_ZONE);
        LocalTime now = LocalTime.now(DASHBOARD_ZONE);
        short dayOfWeek = (short) today.getDayOfWeek().getValue();
        Optional<SemesterEntity> semester = currentSemester(clazz);
        if (semester.isEmpty()) {
            return Optional.empty();
        }
        List<TimetableEntryEntity> entries = timetableRepository
                .findByClassIdAndSemesterIdAndDayOfWeekOrderByPeriodNoAsc(
                        clazz.getId(),
                        semester.get().getId(),
                        dayOfWeek);
        Map<UUID, SubjectEntity> subjects = subjectRepository.findAllById(entries.stream()
                        .map(TimetableEntryEntity::getSubjectId)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(SubjectEntity::getId, Function.identity()));
        return entries.stream()
                .filter(entry -> now.isBefore(entry.getEndTime()))
                .min(Comparator.comparing(TimetableEntryEntity::getStartTime))
                .map(entry -> new ParentDashboardResponse.NextLesson(
                        Optional.ofNullable(subjects.get(entry.getSubjectId()))
                                .map(SubjectEntity::getName)
                                .orElse(null),
                        "Tiết " + entry.getPeriodNo(),
                        entry.getStartTime().format(TIME_FORMATTER)));
    }

    private Optional<SemesterEntity> currentSemester(ClassEntity clazz) {
        return semesterRepository.findBySchoolYearId(clazz.getSchoolYearId()).stream()
                .filter(semester -> Boolean.TRUE.equals(semester.getCurrent()))
                .findFirst()
                .or(() -> schoolYearRepository.findByCurrentTrue()
                        .flatMap(schoolYear -> semesterRepository.findBySchoolYearIdAndCurrentTrue(schoolYear.getId())));
    }

    private Optional<ParentDashboardResponse.HomeroomTeacher> homeroomTeacher(ClassEntity clazz) {
        if (!hasText(clazz.getHomeroomTeacherName())) {
            return Optional.empty();
        }
        return Optional.of(new ParentDashboardResponse.HomeroomTeacher(
                clazz.getHomeroomTeacherName(),
                "Giáo viên chủ nhiệm",
                null,
                null));
    }

    private String roleCode(UserEntity user) {
        RoleEntity role = user.getRole();
        return role == null ? null : role.getCode();
    }

    private boolean isStudent(UserEntity user) {
        return RoleCodes.STUDENT.equals(roleCode(user));
    }

    private boolean samePhone(String first, String second) {
        String normalizedFirst = normalizePhone(first);
        String normalizedSecond = normalizePhone(second);
        return hasText(normalizedFirst) && normalizedFirst.equals(normalizedSecond);
    }

    private String normalizePhone(String phone) {
        if (!hasText(phone)) {
            return null;
        }
        String normalized = phone.trim().replaceAll("[\\s.()-]", "");
        if (normalized.startsWith("+84")) {
            return "0" + normalized.substring(3);
        }
        if (normalized.startsWith("84") && normalized.length() == 11) {
            return "0" + normalized.substring(2);
        }
        return normalized;
    }

    private List<String> phoneCandidates(String phone) {
        String normalized = normalizePhone(phone);
        if (!hasText(normalized)) {
            return List.of();
        }
        if (normalized.startsWith("0") && normalized.length() > 1) {
            String withoutZero = normalized.substring(1);
            return List.of(normalized, "84" + withoutZero, "+84" + withoutZero);
        }
        return List.of(normalized);
    }

    private String avatarText(String fullName) {
        if (!hasText(fullName)) {
            return "";
        }
        String[] parts = fullName.trim().split("\\s+");
        String initials = parts.length == 1
                ? parts[0].substring(0, Math.min(2, parts[0].length()))
                : parts[0].substring(0, 1) + parts[parts.length - 1].charAt(0);
        return Normalizer.normalize(initials, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('Đ', 'D')
                .replace('đ', 'd')
                .toUpperCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
