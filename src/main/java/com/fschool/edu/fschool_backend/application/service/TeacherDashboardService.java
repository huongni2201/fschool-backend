package com.fschool.edu.fschool_backend.application.service;

import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.NotificationEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.ClassEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.ExamEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.SchoolYearEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.SemesterEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.SubjectEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.TeacherProfileEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.TimetableEntryEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.ClassJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.ExamJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.NotificationJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.SchoolYearJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.SemesterJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.StudentRequestJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.SubjectJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.TeacherProfileJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.TimetableEntryJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.UserJpaRepository;
import com.fschool.edu.fschool_backend.presentation.dto.response.TeacherDashboardResponse;
import com.fschool.edu.fschool_backend.presentation.exception.ApiException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
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
public class TeacherDashboardService {

    private final TeacherProfileJpaRepository teacherProfileRepository;
    private final NotificationJpaRepository notificationRepository;
    private final SchoolYearJpaRepository schoolYearRepository;
    private final SemesterJpaRepository semesterRepository;
    private final TimetableEntryJpaRepository timetableEntryRepository;
    private final ClassJpaRepository classRepository;
    private final SubjectJpaRepository subjectRepository;
    private final ExamJpaRepository examRepository;
    private final UserJpaRepository userRepository;
    private final StudentRequestJpaRepository studentRequestRepository;
    private final TeacherGradeAccessService teacherGradeAccessService;

    @Transactional(readOnly = true)
    public TeacherDashboardResponse getDashboard(UUID userId) {
        TeacherProfileEntity teacher = teacherProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Teacher profile not found"));
        Optional<SemesterEntity> currentSemester = currentSemester();
        List<TimetableEntryEntity> semesterEntries = currentSemester
                .map(semester -> teacherSemesterEntries(teacher, semester))
                .orElseGet(List::of);
        List<ClassEntity> homeroomClasses = teacherGradeAccessService.homeroomClasses(teacher);
        List<ClassEntity> managedClasses = classesForEntries(semesterEntries, homeroomClasses);
        List<SubjectEntity> subjects = subjectsForEntries(semesterEntries);

        return new TeacherDashboardResponse(
                toTeacher(teacher),
                todayClasses(teacher, currentSemester, managedClasses, subjects),
                managedClasses(managedClasses, homeroomClasses, semesterEntries, subjects),
                homeroomClasses.stream().findFirst().map(this::toHomeroomClass).orElse(null),
                pendingApplications(homeroomClasses),
                upcomingExams(managedClasses, subjects),
                recentNotifications(teacher.getUserId()),
                tasks(teacher.getUserId(), homeroomClasses));
    }

    private TeacherDashboardResponse.Teacher toTeacher(TeacherProfileEntity teacher) {
        return new TeacherDashboardResponse.Teacher(
                teacher.getId().toString(),
                teacher.getFullName(),
                teacher.getEmployeeCode(),
                teacher.getDepartmentName());
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

    private Optional<SemesterEntity> currentSemester() {
        return schoolYearRepository.findByCurrentTrue()
                .map(SchoolYearEntity::getId)
                .flatMap(semesterRepository::findBySchoolYearIdAndCurrentTrue);
    }

    private List<ClassEntity> classesForEntries(
            List<TimetableEntryEntity> entries,
            List<ClassEntity> homeroomClasses) {
        List<UUID> classIds = entries.stream()
                .map(TimetableEntryEntity::getClassId)
                .collect(Collectors.toSet())
                .stream()
                .toList();
        Map<UUID, ClassEntity> classes = classRepository.findAllById(classIds).stream()
                .collect(Collectors.toMap(ClassEntity::getId, Function.identity()));
        homeroomClasses.forEach(schoolClass -> classes.putIfAbsent(schoolClass.getId(), schoolClass));
        return classes.values().stream()
                .sorted(java.util.Comparator.comparing(ClassEntity::getName))
                .toList();
    }

    private List<SubjectEntity> subjectsForEntries(List<TimetableEntryEntity> entries) {
        return subjectRepository.findAllById(entries.stream()
                        .map(TimetableEntryEntity::getSubjectId)
                        .collect(Collectors.toSet()))
                .stream()
                .toList();
    }

    private List<TeacherDashboardResponse.TodayClass> todayClasses(
            TeacherProfileEntity teacher,
            Optional<SemesterEntity> currentSemester,
            List<ClassEntity> classes,
            List<SubjectEntity> subjects) {
        if (currentSemester.isEmpty()) {
            return List.of();
        }

        short dayOfWeek = (short) LocalDate.now().getDayOfWeek().getValue();
        Map<UUID, ClassEntity> classById = classes.stream()
                .collect(Collectors.toMap(ClassEntity::getId, Function.identity()));
        Map<UUID, SubjectEntity> subjectById = subjects.stream()
                .collect(Collectors.toMap(SubjectEntity::getId, Function.identity()));

        return teacherDayEntries(teacher, currentSemester.get(), dayOfWeek)
                .stream()
                .map(entry -> toTodayClass(entry, classById, subjectById))
                .toList();
    }

    private List<TimetableEntryEntity> teacherSemesterEntries(TeacherProfileEntity teacher, SemesterEntity semester) {
        List<TimetableEntryEntity> entries = timetableEntryRepository
                .findByTeacherIdAndSemesterIdOrderByDayOfWeekAscPeriodNoAsc(teacher.getUserId(), semester.getId());
        if (!entries.isEmpty()) {
            return entries;
        }
        return timetableEntryRepository.findByTeacherNameIgnoreCaseAndSemesterIdOrderByDayOfWeekAscPeriodNoAsc(
                teacher.getFullName(), semester.getId());
    }

    private List<TimetableEntryEntity> teacherDayEntries(
            TeacherProfileEntity teacher,
            SemesterEntity semester,
            short dayOfWeek) {
        List<TimetableEntryEntity> entries = timetableEntryRepository
                .findByTeacherIdAndSemesterIdAndDayOfWeekOrderByStartTimeAscPeriodNoAsc(
                        teacher.getUserId(), semester.getId(), dayOfWeek);
        if (!entries.isEmpty()) {
            return entries;
        }
        return timetableEntryRepository.findByTeacherNameIgnoreCaseAndSemesterIdAndDayOfWeekOrderByStartTimeAscPeriodNoAsc(
                teacher.getFullName(), semester.getId(), dayOfWeek);
    }

    private TeacherDashboardResponse.TodayClass toTodayClass(
            TimetableEntryEntity entry,
            Map<UUID, ClassEntity> classById,
            Map<UUID, SubjectEntity> subjectById) {
        ClassEntity schoolClass = classById.get(entry.getClassId());
        SubjectEntity subject = subjectById.get(entry.getSubjectId());

        return new TeacherDashboardResponse.TodayClass(
                entry.getClassId().toString(),
                schoolClass == null ? "Chưa rõ lớp" : schoolClass.getName(),
                entry.getSubjectId().toString(),
                subject == null ? "Chưa rõ môn" : subject.getName(),
                entry.getRoomName(),
                entry.getStartTime() + " - " + entry.getEndTime(),
                "Theo thời khóa biểu");
    }

    private List<TeacherDashboardResponse.ManagedClass> managedClasses(
            List<ClassEntity> classes,
            List<ClassEntity> homeroomClasses,
            List<TimetableEntryEntity> entries,
            List<SubjectEntity> subjects) {
        Map<UUID, SubjectEntity> subjectById = subjects.stream()
                .collect(Collectors.toMap(SubjectEntity::getId, Function.identity()));

        return classes.stream()
                .map(schoolClass -> new TeacherDashboardResponse.ManagedClass(
                        schoolClass.getId().toString(),
                        schoolClass.getName(),
                        homeroomClasses.stream().anyMatch(item -> item.getId().equals(schoolClass.getId()))
                                ? "Chủ nhiệm"
                                : "Giảng dạy",
                        managedClassSubjectName(schoolClass, homeroomClasses, entries, subjectById),
                        userRepository.countByClassId(schoolClass.getId())))
                .toList();
    }

    private String managedClassSubjectName(
            ClassEntity schoolClass,
            List<ClassEntity> homeroomClasses,
            List<TimetableEntryEntity> entries,
            Map<UUID, SubjectEntity> subjectById) {
        if (homeroomClasses.stream().anyMatch(item -> item.getId().equals(schoolClass.getId()))) {
            return "T\u1EA5t c\u1EA3 m\u00F4n";
        }
        String subjectNames = entries.stream()
                .filter(entry -> schoolClass.getId().equals(entry.getClassId()))
                .map(TimetableEntryEntity::getSubjectId)
                .distinct()
                .map(subjectById::get)
                .filter(java.util.Objects::nonNull)
                .map(SubjectEntity::getName)
                .distinct()
                .collect(Collectors.joining(", "));
        return subjectNames.isBlank() ? "Theo ph\u00E2n c\u00F4ng" : subjectNames;
    }

    private TeacherDashboardResponse.HomeroomClass toHomeroomClass(ClassEntity schoolClass) {
        return new TeacherDashboardResponse.HomeroomClass(
                schoolClass.getId().toString(),
                schoolClass.getName(),
                "Chủ nhiệm",
                userRepository.countByClassId(schoolClass.getId()));
    }

    private long pendingApplications(List<ClassEntity> homeroomClasses) {
        List<UUID> studentIds = userRepository.findByClassIdIn(homeroomClasses.stream()
                        .map(ClassEntity::getId)
                        .collect(Collectors.toSet()))
                .stream()
                .map(user -> user.getId())
                .toList();

        if (studentIds.isEmpty()) {
            return 0;
        }

        return studentRequestRepository.countByStudentIdInAndStatusIn(
                studentIds, List.of(com.fschool.edu.fschool_backend.domain.enums.StudentRequestStatus.SUBMITTED));
    }

    private List<TeacherDashboardResponse.UpcomingExam> upcomingExams(
            List<ClassEntity> classes,
            List<SubjectEntity> subjects) {
        if (classes.isEmpty() || subjects.isEmpty()) {
            return List.of();
        }

        Map<UUID, ClassEntity> classById = classes.stream()
                .collect(Collectors.toMap(ClassEntity::getId, Function.identity()));
        Map<UUID, SubjectEntity> subjectById = subjects.stream()
                .collect(Collectors.toMap(SubjectEntity::getId, Function.identity()));

        return examRepository
                .findByClassIdInAndSubjectIdInAndExamDateGreaterThanEqualOrderByExamDateAscStartTimeAsc(
                        ids(classes), ids(subjects), LocalDate.now())
                .stream()
                .limit(5)
                .map(exam -> toUpcomingExam(exam, classById, subjectById))
                .toList();
    }

    private TeacherDashboardResponse.UpcomingExam toUpcomingExam(
            ExamEntity exam,
            Map<UUID, ClassEntity> classById,
            Map<UUID, SubjectEntity> subjectById) {
        return new TeacherDashboardResponse.UpcomingExam(
                exam.getTitle(),
                Optional.ofNullable(classById.get(exam.getClassId())).map(ClassEntity::getName).orElse("Chưa rõ lớp"),
                Optional.ofNullable(subjectById.get(exam.getSubjectId())).map(SubjectEntity::getName).orElse("Chưa rõ môn"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy").format(exam.getExamDate()));
    }

    private List<TeacherDashboardResponse.Task> tasks(UUID userId, List<ClassEntity> homeroomClasses) {
        long unreadNotifications = notificationRepository.countByUserIdAndReadFalse(userId);
        long pendingRequests = pendingApplications(homeroomClasses);

        return java.util.stream.Stream.of(
                        new TeacherDashboardResponse.Task(
                                "Thông báo chưa đọc",
                                "Các thông báo mới từ hệ thống",
                                unreadNotifications,
                                "NOTIFICATION"),
                        new TeacherDashboardResponse.Task(
                                "Đơn từ học sinh",
                                "Yêu cầu từ lớp chủ nhiệm đang chờ xử lý",
                                pendingRequests,
                                "REQUEST"))
                .filter(task -> task.count() > 0)
                .toList();
    }

    private Collection<UUID> ids(List<? extends com.fschool.edu.fschool_backend.infrastructure.persistence.entity.BaseEntity> entities) {
        return entities.stream().map(com.fschool.edu.fschool_backend.infrastructure.persistence.entity.BaseEntity::getId).toList();
    }
}
