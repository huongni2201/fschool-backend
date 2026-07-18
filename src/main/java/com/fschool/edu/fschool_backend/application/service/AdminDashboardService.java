package com.fschool.edu.fschool_backend.application.service;

import com.fschool.edu.fschool_backend.domain.constants.RoleCodes;
import com.fschool.edu.fschool_backend.domain.enums.StudentRequestStatus;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.ClassEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.ExamEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.NotificationEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.SchoolYearEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.SemesterEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.StudentRequestEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.SubjectEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.ClassJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.ExamJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.GradeJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.NotificationJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.SchoolYearJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.SemesterJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.StudentRequestJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.SubjectJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.TeacherProfileJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.UserJpaRepository;
import com.fschool.edu.fschool_backend.presentation.dto.response.AdminDashboardResponse;
import java.time.LocalDate;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("'Ngày' dd/MM", Locale.forLanguageTag("vi-VN"));

    private final UserJpaRepository userRepository;
    private final TeacherProfileJpaRepository teacherProfileRepository;
    private final ClassJpaRepository classRepository;
    private final NotificationJpaRepository notificationRepository;
    private final StudentRequestJpaRepository studentRequestRepository;
    private final GradeJpaRepository gradeRepository;
    private final ExamJpaRepository examRepository;
    private final SubjectJpaRepository subjectRepository;
    private final SchoolYearJpaRepository schoolYearRepository;
    private final SemesterJpaRepository semesterRepository;

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        Optional<SchoolYearEntity> currentSchoolYear = schoolYearRepository.findByCurrentTrue();
        Optional<SemesterEntity> currentSemester = currentSchoolYear
                .flatMap(schoolYear -> semesterRepository.findBySchoolYearIdAndCurrentTrue(schoolYear.getId()));

        return new AdminDashboardResponse(
                currentSchoolYear.map(SchoolYearEntity::getName).orElse("Chưa cấu hình"),
                currentSemester.map(SemesterEntity::getName).orElse("Chưa cấu hình"),
                userRepository.countByRoleCode(RoleCodes.STUDENT),
                teacherProfileRepository.count(),
                classRepository.count(),
                notificationRepository.countByReadFalse(),
                studentRequestRepository.countByStatus(StudentRequestStatus.SUBMITTED),
                gradeRepository.count(),
                upcomingEvents(),
                recentActivities());
    }

    private List<AdminDashboardResponse.UpcomingEvent> upcomingEvents() {
        List<ExamEntity> exams = examRepository.findByExamDateGreaterThanEqualOrderByExamDateAscStartTimeAsc(LocalDate.now())
                .stream()
                .limit(5)
                .toList();

        Map<UUID, ClassEntity> classes = classRepository.findAllById(exams.stream()
                        .map(ExamEntity::getClassId)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(ClassEntity::getId, Function.identity()));
        Map<UUID, SubjectEntity> subjects = subjectRepository.findAllById(exams.stream()
                        .map(ExamEntity::getSubjectId)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(SubjectEntity::getId, Function.identity()));

        return exams.stream()
                .map(exam -> toUpcomingEvent(exam, classes, subjects))
                .toList();
    }

    private AdminDashboardResponse.UpcomingEvent toUpcomingEvent(
            ExamEntity exam,
            Map<UUID, ClassEntity> classes,
            Map<UUID, SubjectEntity> subjects) {
        String className = Optional.ofNullable(classes.get(exam.getClassId()))
                .map(ClassEntity::getName)
                .orElse("Chưa rõ lớp");
        String subjectName = Optional.ofNullable(subjects.get(exam.getSubjectId()))
                .map(SubjectEntity::getName)
                .orElse("Chưa rõ môn");

        return new AdminDashboardResponse.UpcomingEvent(
                exam.getTitle(),
                className + " - " + subjectName,
                DATE_FORMATTER.format(exam.getExamDate()) + ", " + exam.getStartTime());
    }

    private List<AdminDashboardResponse.RecentActivity> recentActivities() {
        List<AdminDashboardResponse.RecentActivity> requestActivities =
                studentRequestRepository.findTop5ByOrderByCreatedAtDesc().stream()
                        .map(this::toRequestActivity)
                        .toList();
        List<AdminDashboardResponse.RecentActivity> notificationActivities =
                notificationRepository.findTop5ByOrderByCreatedAtDesc().stream()
                        .map(this::toNotificationActivity)
                        .toList();

        return java.util.stream.Stream.concat(requestActivities.stream(), notificationActivities.stream())
                .sorted(Comparator.comparing(AdminDashboardResponse.RecentActivity::timeLabel).reversed())
                .limit(5)
                .toList();
    }

    private AdminDashboardResponse.RecentActivity toRequestActivity(StudentRequestEntity request) {
        return new AdminDashboardResponse.RecentActivity(
                request.getTitle(),
                "Yêu cầu " + request.getRequestNumber() + " đang chờ xử lý",
                request.getCreatedAt().toString(),
                "REQUEST");
    }

    private AdminDashboardResponse.RecentActivity toNotificationActivity(NotificationEntity notification) {
        return new AdminDashboardResponse.RecentActivity(
                notification.getTitle(),
                notification.getBody(),
                notification.getCreatedAt().toString(),
                Boolean.TRUE.equals(notification.getRead()) ? "INFO" : "PENDING");
    }
}
