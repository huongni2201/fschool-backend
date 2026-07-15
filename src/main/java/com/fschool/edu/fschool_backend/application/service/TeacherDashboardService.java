package com.fschool.edu.fschool_backend.application.service;

import com.fschool.edu.fschool_backend.domain.enums.StudentRequestStatus;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.ClassEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.ClassTeacherAssignmentEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.ExamEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.NotificationEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.SemesterEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.SubjectEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.TeacherProfileEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.TeachingAssignmentEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.TimetableEntryEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.UserEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.ClassJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.ClassTeacherAssignmentJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.ExamJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.NotificationJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.SchoolYearJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.SemesterJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.StudentRequestJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.SubjectJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.TeacherProfileJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.TeachingAssignmentJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.TimetableEntryJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.UserJpaRepository;
import com.fschool.edu.fschool_backend.presentation.dto.response.TeacherDashboardResponse;
import com.fschool.edu.fschool_backend.presentation.exception.ApiException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeacherDashboardService {

    private static final String HOMEROOM_TEACHER = "HOMEROOM_TEACHER";
    private static final ZoneId DASHBOARD_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TeacherProfileJpaRepository teacherProfileRepository;
    private final TeachingAssignmentJpaRepository teachingAssignmentRepository;
    private final ClassTeacherAssignmentJpaRepository classTeacherAssignmentRepository;
    private final ClassJpaRepository classRepository;
    private final SubjectJpaRepository subjectRepository;
    private final TimetableEntryJpaRepository timetableRepository;
    private final ExamJpaRepository examRepository;
    private final NotificationJpaRepository notificationRepository;
    private final UserJpaRepository userRepository;
    private final StudentRequestJpaRepository requestRepository;
    private final SchoolYearJpaRepository schoolYearRepository;
    private final SemesterJpaRepository semesterRepository;

    @Transactional(readOnly = true)
    public TeacherDashboardResponse getDashboard(UUID userId) {
        TeacherProfileEntity teacher = teacherProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Teacher profile not found"));
        List<TeachingAssignmentEntity> assignments =
                teachingAssignmentRepository.findByTeacherIdAndActiveTrue(teacher.getId());
        List<ClassTeacherAssignmentEntity> homeroomAssignments =
                classTeacherAssignmentRepository.findByTeacherIdAndRoleAndActiveTrue(
                        teacher.getId(), HOMEROOM_TEACHER);

        Set<UUID> classIds = new LinkedHashSet<>();
        assignments.forEach(assignment -> classIds.add(assignment.getClassId()));
        homeroomAssignments.forEach(assignment -> classIds.add(assignment.getClassId()));
        Map<UUID, ClassEntity> classes = entityMap(classRepository.findAllById(classIds), ClassEntity::getId);

        Set<UUID> subjectIds = assignments.stream()
                .map(TeachingAssignmentEntity::getSubjectId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<UUID, SubjectEntity> subjects = entityMap(subjectRepository.findAllById(subjectIds), SubjectEntity::getId);

        long pendingApplications = pendingApplications(homeroomAssignments);
        List<TeacherDashboardResponse.UpcomingExam> upcomingExams =
                upcomingExams(assignments, classes, subjects);

        return new TeacherDashboardResponse(
                toTeacher(teacher),
                todayClasses(assignments, classes, subjects),
                managedClasses(assignments, classes, subjects),
                homeroomClass(homeroomAssignments, classes).orElse(null),
                pendingApplications,
                upcomingExams,
                recentNotifications(teacher.getUserId()),
                tasks(pendingApplications, upcomingExams.size()));
    }

    private TeacherDashboardResponse.Teacher toTeacher(TeacherProfileEntity teacher) {
        return new TeacherDashboardResponse.Teacher(
                teacher.getId().toString(),
                teacher.getFullName(),
                teacher.getEmployeeCode(),
                teacher.getDepartmentName());
    }

    private List<TeacherDashboardResponse.TodayClass> todayClasses(
            List<TeachingAssignmentEntity> assignments,
            Map<UUID, ClassEntity> classes,
            Map<UUID, SubjectEntity> subjects) {
        Optional<SemesterEntity> semester = currentSemester();
        if (semester.isEmpty() || assignments.isEmpty()) {
            return List.of();
        }
        LocalDate today = LocalDate.now(DASHBOARD_ZONE);
        short dayOfWeek = (short) today.getDayOfWeek().getValue();
        Set<UUID> classIds = assignments.stream()
                .map(TeachingAssignmentEntity::getClassId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<UUID> subjectIds = assignments.stream()
                .map(TeachingAssignmentEntity::getSubjectId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> assignmentPairs = assignmentPairs(assignments);
        return timetableRepository
                .findByClassIdInAndSemesterIdAndSubjectIdInAndDayOfWeekOrderByStartTimeAscPeriodNoAsc(
                        classIds,
                        semester.get().getId(),
                        subjectIds,
                        dayOfWeek)
                .stream()
                .filter(entry -> assignmentPairs.contains(pair(entry.getClassId(), entry.getSubjectId())))
                .map(entry -> toTodayClass(entry, classes.get(entry.getClassId()), subjects.get(entry.getSubjectId())))
                .toList();
    }

    private TeacherDashboardResponse.TodayClass toTodayClass(
            TimetableEntryEntity entry,
            ClassEntity clazz,
            SubjectEntity subject) {
        return new TeacherDashboardResponse.TodayClass(
                entry.getClassId().toString(),
                clazz == null ? null : clazz.getName(),
                subjectClientId(entry.getSubjectId(), subject),
                subject == null ? null : subject.getName(),
                entry.getRoomName(),
                entry.getStartTime().format(TIME_FORMATTER) + " - " + entry.getEndTime().format(TIME_FORMATTER),
                "Ti\u1EBFt " + entry.getPeriodNo());
    }

    private List<TeacherDashboardResponse.ManagedClass> managedClasses(
            List<TeachingAssignmentEntity> assignments,
            Map<UUID, ClassEntity> classes,
            Map<UUID, SubjectEntity> subjects) {
        return assignments.stream()
                .sorted(Comparator
                        .comparing((TeachingAssignmentEntity assignment) ->
                                className(classes.get(assignment.getClassId())))
                        .thenComparing(assignment -> subjectName(subjects.get(assignment.getSubjectId()))))
                .map(assignment -> {
                    ClassEntity clazz = classes.get(assignment.getClassId());
                    SubjectEntity subject = subjects.get(assignment.getSubjectId());
                    return new TeacherDashboardResponse.ManagedClass(
                            assignment.getClassId().toString(),
                            clazz == null ? null : clazz.getName(),
                            "Gi\u1EA3ng d\u1EA1y",
                            subject == null ? null : subject.getName(),
                            userRepository.countByClassId(assignment.getClassId()));
                })
                .toList();
    }

    private Optional<TeacherDashboardResponse.HomeroomClass> homeroomClass(
            List<ClassTeacherAssignmentEntity> homeroomAssignments,
            Map<UUID, ClassEntity> classes) {
        return homeroomAssignments.stream()
                .findFirst()
                .map(assignment -> {
                    ClassEntity clazz = classes.get(assignment.getClassId());
                    return new TeacherDashboardResponse.HomeroomClass(
                            assignment.getClassId().toString(),
                            clazz == null ? null : clazz.getName(),
                            "Gi\u00E1o vi\u00EAn ch\u1EE7 nhi\u1EC7m",
                            userRepository.countByClassId(assignment.getClassId()));
                });
    }

    private long pendingApplications(List<ClassTeacherAssignmentEntity> homeroomAssignments) {
        Set<UUID> homeroomClassIds = homeroomAssignments.stream()
                .map(ClassTeacherAssignmentEntity::getClassId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (homeroomClassIds.isEmpty()) {
            return 0;
        }
        List<UUID> studentIds = userRepository.findByClassIdIn(homeroomClassIds).stream()
                .map(UserEntity::getId)
                .toList();
        if (studentIds.isEmpty()) {
            return 0;
        }
        return requestRepository.countByStudentIdInAndStatusIn(
                studentIds,
                List.of(StudentRequestStatus.SUBMITTED, StudentRequestStatus.PROCESSING));
    }

    private List<TeacherDashboardResponse.UpcomingExam> upcomingExams(
            List<TeachingAssignmentEntity> assignments,
            Map<UUID, ClassEntity> classes,
            Map<UUID, SubjectEntity> subjects) {
        if (assignments.isEmpty()) {
            return List.of();
        }
        Set<UUID> classIds = assignments.stream()
                .map(TeachingAssignmentEntity::getClassId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<UUID> subjectIds = assignments.stream()
                .map(TeachingAssignmentEntity::getSubjectId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> assignmentPairs = assignmentPairs(assignments);
        return examRepository
                .findByClassIdInAndSubjectIdInAndExamDateGreaterThanEqualOrderByExamDateAscStartTimeAsc(
                        classIds,
                        subjectIds,
                        LocalDate.now(DASHBOARD_ZONE))
                .stream()
                .filter(exam -> assignmentPairs.contains(pair(exam.getClassId(), exam.getSubjectId())))
                .limit(5)
                .map(exam -> toUpcomingExam(exam, classes.get(exam.getClassId()), subjects.get(exam.getSubjectId())))
                .toList();
    }

    private TeacherDashboardResponse.UpcomingExam toUpcomingExam(
            ExamEntity exam,
            ClassEntity clazz,
            SubjectEntity subject) {
        return new TeacherDashboardResponse.UpcomingExam(
                exam.getTitle(),
                clazz == null ? null : clazz.getName(),
                subject == null ? null : subject.getName(),
                exam.getExamDate().format(DATE_FORMATTER));
    }

    private List<TeacherDashboardResponse.RecentNotification> recentNotifications(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .limit(5)
                .map(this::toRecentNotification)
                .toList();
    }

    private TeacherDashboardResponse.RecentNotification toRecentNotification(NotificationEntity notification) {
        return new TeacherDashboardResponse.RecentNotification(
                notification.getTitle(),
                notification.getBody(),
                notification.getNotificationType());
    }

    private List<TeacherDashboardResponse.Task> tasks(long pendingApplications, int upcomingExamCount) {
        List<TeacherDashboardResponse.Task> tasks = new ArrayList<>();
        if (pendingApplications > 0) {
            tasks.add(new TeacherDashboardResponse.Task(
                    "X\u1EED l\u00FD \u0111\u01A1n h\u1ECDc sinh",
                    "C\u00F3 \u0111\u01A1n c\u1EA7n gi\u00E1o vi\u00EAn ch\u1EE7 nhi\u1EC7m xem x\u00E9t.",
                    pendingApplications,
                    "APPLICATION"));
        }
        if (upcomingExamCount > 0) {
            tasks.add(new TeacherDashboardResponse.Task(
                    "Theo d\u00F5i l\u1ECBch ki\u1EC3m tra",
                    "C\u00F3 l\u1ECBch ki\u1EC3m tra s\u1EAFp t\u1EDBi cho l\u1EDBp ph\u1EE5 tr\u00E1ch.",
                    upcomingExamCount,
                    "EXAM"));
        }
        return tasks;
    }

    private Optional<SemesterEntity> currentSemester() {
        return schoolYearRepository.findByCurrentTrue()
                .flatMap(schoolYear -> semesterRepository.findBySchoolYearIdAndCurrentTrue(schoolYear.getId()));
    }

    private Set<String> assignmentPairs(List<TeachingAssignmentEntity> assignments) {
        return assignments.stream()
                .map(assignment -> pair(assignment.getClassId(), assignment.getSubjectId()))
                .collect(Collectors.toSet());
    }

    private String pair(UUID classId, UUID subjectId) {
        return classId + ":" + subjectId;
    }

    private String subjectClientId(UUID subjectId, SubjectEntity subject) {
        if (subject != null && subject.getCode() != null && !subject.getCode().isBlank()) {
            return subject.getCode().toLowerCase(java.util.Locale.ROOT);
        }
        return subjectId.toString();
    }

    private String className(ClassEntity clazz) {
        return clazz == null ? "" : clazz.getName();
    }

    private String subjectName(SubjectEntity subject) {
        return subject == null ? "" : subject.getName();
    }

    private <T> Map<UUID, T> entityMap(Iterable<T> entities, Function<T, UUID> idExtractor) {
        return stream(entities).collect(Collectors.toMap(idExtractor, Function.identity()));
    }

    private <T> java.util.stream.Stream<T> stream(Iterable<T> values) {
        return values instanceof Collection<T> collection
                ? collection.stream()
                : java.util.stream.StreamSupport.stream(values.spliterator(), false);
    }
}
