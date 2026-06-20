package com.fschool.edu.fschool_backend.application.service;

import com.fschool.edu.fschool_backend.presentation.dto.response.AssignmentResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.ClassSummaryResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.CountResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.CurrentSemesterResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.ExamResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.GradeItemResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.HomeStudentResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.HomeSummaryResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.LessonResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.NewsDetailResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.NewsListItemResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.NewsPageResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.NotificationResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.SchoolYearResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.SubjectGradesResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.SubjectResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.TimetableDayResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.UserMeResponse;
import com.fschool.edu.fschool_backend.application.command.UpdateMeCommand;
import com.fschool.edu.fschool_backend.presentation.exception.ApiException;
import com.fschool.edu.fschool_backend.domain.enums.AssignmentStatus;
import com.fschool.edu.fschool_backend.domain.enums.ContentStatus;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.AssignmentEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.ClassEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.ExamEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.GradeEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.NewsPostEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.NotificationEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.SchoolYearEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.SemesterEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.SubjectEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.TimetableEntryEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.UserEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.AssignmentJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.ClassJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.ExamJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.GradeJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.NewsPostJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.NotificationJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.SchoolYearJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.SemesterJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.SubjectJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.TimetableEntryJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.UserJpaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentPortalService {

    private final UserJpaRepository userRepository;
    private final ClassJpaRepository classRepository;
    private final SchoolYearJpaRepository schoolYearRepository;
    private final SemesterJpaRepository semesterRepository;
    private final SubjectJpaRepository subjectRepository;
    private final TimetableEntryJpaRepository timetableRepository;
    private final GradeJpaRepository gradeRepository;
    private final AssignmentJpaRepository assignmentRepository;
    private final ExamJpaRepository examRepository;
    private final NewsPostJpaRepository newsRepository;
    private final NotificationJpaRepository notificationRepository;

    public StudentPortalService(
            UserJpaRepository userRepository,
            ClassJpaRepository classRepository,
            SchoolYearJpaRepository schoolYearRepository,
            SemesterJpaRepository semesterRepository,
            SubjectJpaRepository subjectRepository,
            TimetableEntryJpaRepository timetableRepository,
            GradeJpaRepository gradeRepository,
            AssignmentJpaRepository assignmentRepository,
            ExamJpaRepository examRepository,
            NewsPostJpaRepository newsRepository,
            NotificationJpaRepository notificationRepository) {
        this.userRepository = userRepository;
        this.classRepository = classRepository;
        this.schoolYearRepository = schoolYearRepository;
        this.semesterRepository = semesterRepository;
        this.subjectRepository = subjectRepository;
        this.timetableRepository = timetableRepository;
        this.gradeRepository = gradeRepository;
        this.assignmentRepository = assignmentRepository;
        this.examRepository = examRepository;
        this.newsRepository = newsRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public UserMeResponse getMe(UUID userId) {
        UserEntity user = requireUser(userId);
        ClassEntity clazz = findClass(user.getClassId()).orElse(null);
        return toUserMe(user, clazz);
    }

    @Transactional
    public UserMeResponse updateMe(UUID userId, UpdateMeCommand command) {
        UserEntity user = requireUser(userId);
        user.setAvatarUrl(command.avatarUrl());
        user.setAddress(command.address());
        UserEntity saved = userRepository.save(user);
        return toUserMe(saved, findClass(saved.getClassId()).orElse(null));
    }

    @Transactional(readOnly = true)
    public HomeSummaryResponse getHomeSummary(UUID userId) {
        UserEntity user = requireUser(userId);
        ClassEntity clazz = requireClass(user);
        SemesterEntity semester = getCurrentSemesterEntity();
        Map<UUID, SubjectEntity> subjects = subjectMap();
        LocalDate today = LocalDate.now();
        short dayOfWeek = (short) today.getDayOfWeek().getValue();
        List<LessonResponse> lessons = timetableRepository
                .findByClassIdAndSemesterIdAndDayOfWeekOrderByPeriodNoAsc(clazz.getId(), semester.getId(), dayOfWeek)
                .stream()
                .map(entry -> toLesson(entry, subjects.get(entry.getSubjectId())))
                .toList();
        LessonResponse currentLesson = lessons.stream().findFirst().orElse(null);
        List<GradeItemResponse> recentGrades = gradeRepository.findByUserIdOrderByAssessmentDateDesc(user.getId())
                .stream().limit(5).map(this::toGradeItem).toList();
        Instant now = Instant.now();
        List<AssignmentResponse> assignments = assignmentRepository.findByClassId(clazz.getId()).stream()
                .filter(item -> item.getStatus() == AssignmentStatus.PUBLISHED)
                .filter(item -> item.getDueAt() == null || item.getDueAt().isAfter(now))
                .sorted(Comparator.comparing(AssignmentEntity::getDueAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(5)
                .map(item -> toAssignment(item, subjects.get(item.getSubjectId())))
                .toList();
        List<ExamResponse> exams = examRepository.findByClassId(clazz.getId()).stream()
                .filter(item -> !item.getExamDate().isBefore(today))
                .sorted(Comparator.comparing(ExamEntity::getExamDate).thenComparing(ExamEntity::getStartTime))
                .limit(5)
                .map(item -> toExam(item, subjects.get(item.getSubjectId())))
                .toList();
        List<NewsListItemResponse> latestNews = newsRepository.findByStatusOrderByPublishedAtDesc(ContentStatus.PUBLISHED)
                .stream().limit(5).map(this::toNewsListItem).toList();
        long unreadCount = notificationRepository.countByUserIdAndRead(user.getId(), false);
        return new HomeSummaryResponse(
                new HomeStudentResponse(user.getFullName(), user.getStudentCode(), clazz.getName(), user.getAvatarUrl()),
                toCurrentSemester(semester),
                currentLesson,
                List.of(new TimetableDayResponse(dayOfWeek, today, lessons)),
                recentGrades,
                assignments,
                exams,
                latestNews,
                unreadCount);
    }

    @Transactional(readOnly = true)
    public SchoolYearResponse getCurrentSchoolYear() {
        return toSchoolYear(getCurrentSchoolYearEntity());
    }

    @Transactional(readOnly = true)
    public CurrentSemesterResponse getCurrentSemester() {
        return toCurrentSemester(getCurrentSemesterEntity());
    }

    @Transactional(readOnly = true)
    public List<CurrentSemesterResponse> getSemesters() {
        Map<UUID, SchoolYearEntity> schoolYears = schoolYearRepository.findAll().stream()
                .collect(Collectors.toMap(SchoolYearEntity::getId, Function.identity()));
        return semesterRepository.findAll().stream()
                .sorted(Comparator.comparing(SemesterEntity::getStartDate))
                .map(semester -> {
                    SchoolYearEntity schoolYear = schoolYears.get(semester.getSchoolYearId());
                    return new CurrentSemesterResponse(
                            semester.getId(),
                            semester.getName(),
                            semester.getSemesterNo(),
                            schoolYear == null ? null : schoolYear.getName(),
                            semester.getCurrent());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public TimetableDayResponse getTimetableForDate(UUID userId, LocalDate date) {
        UserEntity user = requireUser(userId);
        ClassEntity clazz = requireClass(user);
        SemesterEntity semester = getCurrentSemesterEntity();
        short dayOfWeek = (short) date.getDayOfWeek().getValue();
        Map<UUID, SubjectEntity> subjects = subjectMap();
        List<LessonResponse> lessons = timetableRepository
                .findByClassIdAndSemesterIdAndDayOfWeekOrderByPeriodNoAsc(clazz.getId(), semester.getId(), dayOfWeek)
                .stream()
                .map(entry -> toLesson(entry, subjects.get(entry.getSubjectId())))
                .toList();
        return new TimetableDayResponse(dayOfWeek, date, lessons);
    }

    @Transactional(readOnly = true)
    public List<TimetableDayResponse> getTimetableWeek(UUID userId, LocalDate startDate) {
        LocalDate monday = (startDate == null ? LocalDate.now() : startDate)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return java.util.stream.IntStream.range(0, 7)
                .mapToObj(monday::plusDays)
                .map(date -> getTimetableForDate(userId, date))
                .toList();
    }

    @Transactional(readOnly = true)
    public LessonResponse getLesson(UUID userId, UUID lessonId) {
        UserEntity user = requireUser(userId);
        ClassEntity clazz = requireClass(user);
        TimetableEntryEntity entry = timetableRepository.findById(lessonId)
                .orElseThrow(() -> notFound("Timetable entry was not found"));
        if (!entry.getClassId().equals(clazz.getId())) {
            throw notFound("Timetable entry was not found");
        }
        SubjectEntity subject = subjectRepository.findById(entry.getSubjectId()).orElse(null);
        return toLesson(entry, subject);
    }

    @Transactional(readOnly = true)
    public List<SubjectGradesResponse> getGrades(UUID userId, UUID semesterId, UUID subjectId) {
        List<GradeEntity> grades = subjectId == null
                ? gradeRepository.findByUserIdAndSemesterIdOrderByAssessmentDateDesc(userId, semesterId)
                : gradeRepository.findByUserIdAndSubjectIdAndSemesterId(userId, subjectId, semesterId);
        Map<UUID, SubjectEntity> subjects = subjectMap();
        Map<UUID, List<GradeEntity>> bySubject = grades.stream()
                .collect(Collectors.groupingBy(GradeEntity::getSubjectId, LinkedHashMap::new, Collectors.toList()));
        return bySubject.entrySet().stream()
                .map(entry -> {
                    SubjectEntity subject = subjects.get(entry.getKey());
                    List<GradeItemResponse> items = entry.getValue().stream().map(this::toGradeItem).toList();
                    return new SubjectGradesResponse(
                            entry.getKey(),
                            subject == null ? null : subject.getCode(),
                            subject == null ? null : subject.getName(),
                            average(entry.getValue()),
                            items);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GradeItemResponse> getRecentGrades(UUID userId, int limit) {
        return gradeRepository.findByUserIdOrderByAssessmentDateDesc(userId).stream()
                .limit(Math.max(1, limit))
                .map(this::toGradeItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public GradeItemResponse getGrade(UUID userId, UUID gradeId) {
        GradeEntity grade = gradeRepository.findById(gradeId).orElseThrow(() -> notFound("Grade was not found"));
        if (!grade.getUserId().equals(userId)) {
            throw notFound("Grade was not found");
        }
        return toGradeItem(grade);
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAssignments(
            UUID userId, AssignmentStatus status, UUID subjectId, UUID semesterId, boolean upcoming) {
        ClassEntity clazz = requireClass(requireUser(userId));
        Map<UUID, SubjectEntity> subjects = subjectMap();
        Instant now = Instant.now();
        return assignmentRepository.findByClassId(clazz.getId()).stream()
                .filter(item -> status == null || item.getStatus() == status)
                .filter(item -> subjectId == null || item.getSubjectId().equals(subjectId))
                .filter(item -> semesterId == null || item.getSemesterId().equals(semesterId))
                .filter(item -> !upcoming || item.getDueAt() == null || item.getDueAt().isAfter(now))
                .sorted(Comparator.comparing(AssignmentEntity::getDueAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(item -> toAssignment(item, subjects.get(item.getSubjectId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public AssignmentResponse getAssignment(UUID userId, UUID assignmentId) {
        ClassEntity clazz = requireClass(requireUser(userId));
        AssignmentEntity assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> notFound("Assignment was not found"));
        if (!assignment.getClassId().equals(clazz.getId())) {
            throw notFound("Assignment was not found");
        }
        return toAssignment(assignment, subjectRepository.findById(assignment.getSubjectId()).orElse(null));
    }

    @Transactional(readOnly = true)
    public List<ExamResponse> getExams(UUID userId, UUID semesterId, UUID subjectId, boolean upcoming) {
        ClassEntity clazz = requireClass(requireUser(userId));
        Map<UUID, SubjectEntity> subjects = subjectMap();
        LocalDate today = LocalDate.now();
        return examRepository.findByClassId(clazz.getId()).stream()
                .filter(item -> semesterId == null || item.getSemesterId().equals(semesterId))
                .filter(item -> subjectId == null || item.getSubjectId().equals(subjectId))
                .filter(item -> !upcoming || !item.getExamDate().isBefore(today))
                .sorted(Comparator.comparing(ExamEntity::getExamDate).thenComparing(ExamEntity::getStartTime))
                .map(item -> toExam(item, subjects.get(item.getSubjectId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ExamResponse getExam(UUID userId, UUID examId) {
        ClassEntity clazz = requireClass(requireUser(userId));
        ExamEntity exam = examRepository.findById(examId).orElseThrow(() -> notFound("Exam was not found"));
        if (!exam.getClassId().equals(clazz.getId())) {
            throw notFound("Exam was not found");
        }
        return toExam(exam, subjectRepository.findById(exam.getSubjectId()).orElse(null));
    }

    @Transactional(readOnly = true)
    public NewsPageResponse getNews(int page, int size) {
        Page<NewsPostEntity> result = newsRepository.findByStatusOrderByPublishedAtDesc(
                ContentStatus.PUBLISHED, PageRequest.of(Math.max(0, page), Math.max(1, size)));
        return new NewsPageResponse(
                result.getContent().stream().map(this::toNewsListItem).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public NewsDetailResponse getNewsDetail(UUID id) {
        NewsPostEntity news = newsRepository.findById(id).orElseThrow(() -> notFound("News post was not found"));
        if (news.getStatus() != ContentStatus.PUBLISHED) {
            throw notFound("News post was not found");
        }
        return new NewsDetailResponse(
                news.getId(), news.getTitle(), news.getSummary(), news.getContent(), news.getThumbnailUrl(), news.getPublishedAt());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(UUID userId, Boolean isRead) {
        List<NotificationEntity> notifications = isRead == null
                ? notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                : notificationRepository.findByUserIdAndReadOrderByCreatedAtDesc(userId, isRead);
        return notifications.stream().map(this::toNotification).toList();
    }

    @Transactional(readOnly = true)
    public CountResponse getUnreadNotificationCount(UUID userId) {
        return new CountResponse(notificationRepository.countByUserIdAndRead(userId, false));
    }

    @Transactional
    public void markNotificationRead(UUID userId, UUID notificationId) {
        NotificationEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> notFound("Notification was not found"));
        if (!notification.getUserId().equals(userId)) {
            throw notFound("Notification was not found");
        }
        notification.setRead(true);
        notification.setReadAt(Instant.now());
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllNotificationsRead(UUID userId) {
        notificationRepository.findByUserIdAndReadOrderByCreatedAtDesc(userId, false).forEach(notification -> {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
        });
    }

    @Transactional
    public void deleteNotification(UUID userId, UUID notificationId) {
        NotificationEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> notFound("Notification was not found"));
        if (!notification.getUserId().equals(userId)) {
            throw notFound("Notification was not found");
        }
        notificationRepository.delete(notification);
    }

    @Transactional(readOnly = true)
    public List<SubjectResponse> getSubjects() {
        return subjectRepository.findAll().stream()
                .sorted(Comparator.comparing(SubjectEntity::getCode))
                .map(this::toSubject)
                .toList();
    }

    @Transactional(readOnly = true)
    public SubjectResponse getSubject(UUID id) {
        return toSubject(subjectRepository.findById(id).orElseThrow(() -> notFound("Subject was not found")));
    }

    private UserEntity requireUser(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> notFound("User was not found"));
    }

    private ClassEntity requireClass(UserEntity user) {
        return findClass(user.getClassId()).orElseThrow(() -> notFound("Class was not found"));
    }

    private Optional<ClassEntity> findClass(UUID classId) {
        return classId == null ? Optional.empty() : classRepository.findById(classId);
    }

    private SchoolYearEntity getCurrentSchoolYearEntity() {
        return schoolYearRepository.findByCurrentTrue().orElseThrow(() -> notFound("Current school year was not found"));
    }

    private SemesterEntity getCurrentSemesterEntity() {
        SchoolYearEntity schoolYear = getCurrentSchoolYearEntity();
        return semesterRepository.findBySchoolYearIdAndCurrentTrue(schoolYear.getId())
                .orElseThrow(() -> notFound("Current semester was not found"));
    }

    private Map<UUID, SubjectEntity> subjectMap() {
        return subjectRepository.findAll().stream().collect(Collectors.toMap(SubjectEntity::getId, Function.identity()));
    }

    private UserMeResponse toUserMe(UserEntity user, ClassEntity clazz) {
        ClassSummaryResponse classSummary = clazz == null
                ? null
                : new ClassSummaryResponse(clazz.getId(), clazz.getName(), clazz.getGradeNumber(), clazz.getRoomName(), clazz.getHomeroomTeacherName());
        return new UserMeResponse(
                user.getId(),
                user.getPhone(),
                user.getStudentCode(),
                user.getFullName(),
                user.getDateOfBirth(),
                user.getGender(),
                user.getAvatarUrl(),
                user.getAddress(),
                user.getGuardianName(),
                user.getGuardianPhone(),
                classSummary);
    }

    private SchoolYearResponse toSchoolYear(SchoolYearEntity schoolYear) {
        return new SchoolYearResponse(
                schoolYear.getId(),
                schoolYear.getName(),
                schoolYear.getStartDate(),
                schoolYear.getEndDate(),
                schoolYear.getCurrent());
    }

    private CurrentSemesterResponse toCurrentSemester(SemesterEntity semester) {
        SchoolYearEntity schoolYear = schoolYearRepository.findById(semester.getSchoolYearId()).orElse(null);
        return new CurrentSemesterResponse(
                semester.getId(),
                semester.getName(),
                semester.getSemesterNo(),
                schoolYear == null ? null : schoolYear.getName(),
                semester.getCurrent());
    }

    private LessonResponse toLesson(TimetableEntryEntity entry, SubjectEntity subject) {
        return new LessonResponse(
                entry.getId(),
                entry.getPeriodNo(),
                subject == null ? null : subject.getCode(),
                subject == null ? null : subject.getName(),
                entry.getStartTime(),
                entry.getEndTime(),
                entry.getTeacherName(),
                entry.getRoomName());
    }

    private GradeItemResponse toGradeItem(GradeEntity grade) {
        return new GradeItemResponse(
                grade.getId(),
                grade.getTitle(),
                grade.getGradeType(),
                grade.getScore(),
                grade.getMaxScore(),
                grade.getWeight(),
                grade.getAssessmentDate(),
                grade.getComment());
    }

    private BigDecimal average(List<GradeEntity> grades) {
        BigDecimal weightedTotal = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (GradeEntity grade : grades) {
            if (grade.getScore() != null) {
                weightedTotal = weightedTotal.add(grade.getScore().multiply(grade.getWeight()));
                totalWeight = totalWeight.add(grade.getWeight());
            }
        }
        return totalWeight.compareTo(BigDecimal.ZERO) == 0
                ? null
                : weightedTotal.divide(totalWeight, 2, RoundingMode.HALF_UP);
    }

    private AssignmentResponse toAssignment(AssignmentEntity assignment, SubjectEntity subject) {
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getTitle(),
                assignment.getDescription(),
                subject == null ? null : subject.getName(),
                assignment.getTeacherName(),
                assignment.getAttachmentUrl(),
                assignment.getDueAt(),
                assignment.getStatus(),
                assignment.getDueAt() != null && assignment.getDueAt().isBefore(Instant.now()));
    }

    private ExamResponse toExam(ExamEntity exam, SubjectEntity subject) {
        return new ExamResponse(
                exam.getId(),
                exam.getTitle(),
                subject == null ? null : subject.getName(),
                exam.getExamType(),
                exam.getExamDate(),
                exam.getStartTime(),
                exam.getDurationMinutes(),
                exam.getRoomName(),
                exam.getNote());
    }

    private NewsListItemResponse toNewsListItem(NewsPostEntity news) {
        return new NewsListItemResponse(
                news.getId(), news.getTitle(), news.getSummary(), news.getThumbnailUrl(), news.getPublishedAt());
    }

    private NotificationResponse toNotification(NotificationEntity notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getBody(),
                notification.getNotificationType(),
                notification.getDeepLink(),
                notification.getRead(),
                notification.getReadAt(),
                notification.getCreatedAt());
    }

    private SubjectResponse toSubject(SubjectEntity subject) {
        return new SubjectResponse(subject.getId(), subject.getCode(), subject.getName(), subject.getScoreBased());
    }

    private ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }
}
