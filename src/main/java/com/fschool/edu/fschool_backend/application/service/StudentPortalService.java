package com.fschool.edu.fschool_backend.application.service;

import com.fschool.edu.fschool_backend.presentation.dto.response.AcademicPeriodResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.AssignmentResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.ClassSummaryResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.CountResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.CurrentSemesterResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.ExamResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.GradeItemResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.GradeSubjectDetailResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.GradeSummaryResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.HomeStudentResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.HomeSummaryResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.LessonResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.NewsDetailResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.NewsListItemResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.NewsPageResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.NotificationResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.SchoolYearResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentDashboardResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.SubjectGradesResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.SubjectResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.TimetableDayResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.UserMeResponse;
import com.fschool.edu.fschool_backend.application.command.UpdateMeCommand;
import com.fschool.edu.fschool_backend.presentation.exception.ApiException;
import com.fschool.edu.fschool_backend.domain.enums.AssignmentStatus;
import com.fschool.edu.fschool_backend.domain.enums.ContentStatus;
import com.fschool.edu.fschool_backend.domain.enums.GradeType;
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
import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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

    private static final DateTimeFormatter LESSON_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final String LESSON_STATUS_DONE = "done";
    private static final String LESSON_STATUS_LIVE = "live";
    private static final String LESSON_STATUS_UPCOMING = "upcoming";
    private static final ZoneId DASHBOARD_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

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
    public StudentDashboardResponse getStudentDashboard(UUID userId) {
        UserEntity user = requireUser(userId);
        Map<UUID, SubjectEntity> subjects = subjectMap();
        LocalDate today = LocalDate.now(DASHBOARD_ZONE);
        LocalTime now = LocalTime.now(DASHBOARD_ZONE);
        short dayOfWeek = (short) today.getDayOfWeek().getValue();
        List<StudentDashboardResponse.ScheduleItem> todaySchedule = findClass(user.getClassId())
                .flatMap(clazz -> findCurrentSemesterEntity()
                        .map(semester -> timetableRepository
                                .findByClassIdAndSemesterIdAndDayOfWeekOrderByPeriodNoAsc(
                                        clazz.getId(), semester.getId(), dayOfWeek)
                                .stream()
                                .map(entry -> toDashboardScheduleItem(entry, subjects.get(entry.getSubjectId()), now))
                                .toList()))
                .orElseGet(List::of);
        StudentDashboardResponse.CurrentLesson currentLesson = todaySchedule.stream()
                .filter(item -> LESSON_STATUS_LIVE.equals(item.status()))
                .findFirst()
                .or(() -> todaySchedule.stream()
                        .filter(item -> LESSON_STATUS_UPCOMING.equals(item.status()))
                        .findFirst())
                .or(() -> todaySchedule.isEmpty()
                        ? Optional.empty()
                        : Optional.of(todaySchedule.get(todaySchedule.size() - 1)))
                .map(this::toDashboardCurrentLesson)
                .orElse(null);
        List<StudentDashboardResponse.RecentGrade> recentGrades = gradeRepository
                .findByUserIdOrderByAssessmentDateDesc(user.getId())
                .stream()
                .limit(5)
                .map(grade -> toDashboardGrade(grade, subjects.get(grade.getSubjectId())))
                .toList();

        return new StudentDashboardResponse(
                new StudentDashboardResponse.StudentInfo(user.getFullName(), user.getStudentCode()),
                todayTitle(today),
                todaySchedule.size(),
                currentLesson,
                todaySchedule,
                recentGrades);
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
    public List<AcademicPeriodResponse> getAcademicPeriods(UUID studentId) {
        UserEntity student = requireUser(studentId);
        SchoolYearEntity schoolYear = resolveAcademicPeriodSchoolYear(student);
        List<AcademicPeriodResponse> periods = semesterRepository.findBySchoolYearId(schoolYear.getId()).stream()
                .sorted(Comparator.comparing(SemesterEntity::getSemesterNo)
                        .thenComparing(SemesterEntity::getStartDate))
                .map(semester -> toAcademicPeriod(semester, schoolYear))
                .collect(Collectors.toCollection(ArrayList::new));
        periods.add(toAcademicYearPeriod(schoolYear, periods.size() + 1));
        return periods;
    }

    @Transactional(readOnly = true)
    public GradeSummaryResponse getGradeSummary(UUID studentId, String periodId) {
        UserEntity student = requireUser(studentId);
        PeriodResolution period = resolvePeriod(periodId);
        List<UUID> semesterIds = period.semesters().stream().map(SemesterEntity::getId).toList();
        List<GradeEntity> grades = semesterIds.isEmpty()
                ? List.of()
                : gradeRepository.findByUserIdAndSemesterIdInOrderByAssessmentDateDesc(student.getId(), semesterIds);
        Map<UUID, SubjectEntity> subjects = subjectMap();
        Map<UUID, String> teacherNames = teacherNameMap(student, period.semesters());
        Map<UUID, List<GradeEntity>> gradesBySubject = grades.stream()
                .filter(grade -> grade.getScore() != null)
                .collect(Collectors.groupingBy(GradeEntity::getSubjectId, LinkedHashMap::new, Collectors.toList()));
        List<GradeSummaryResponse.SubjectSummary> subjectSummaries = gradesBySubject.entrySet().stream()
                .map(entry -> toGradeSummarySubject(
                        entry.getKey(), entry.getValue(), subjects.get(entry.getKey()), teacherNames))
                .filter(subject -> subject.average() != null)
                .sorted(Comparator
                        .comparingInt((GradeSummaryResponse.SubjectSummary subject) ->
                                subjectDisplayOrder(subject.subjectId()))
                        .thenComparing(GradeSummaryResponse.SubjectSummary::subjectName,
                                Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
        BigDecimal overallAverage = averageSubjectScores(subjectSummaries);
        GradeSummaryResponse.SubjectAverage strongestSubject = subjectSummaries.stream()
                .max(Comparator.comparing(GradeSummaryResponse.SubjectSummary::average))
                .map(subject -> new GradeSummaryResponse.SubjectAverage(
                        subject.subjectId(), subject.subjectName(), subject.average()))
                .orElse(null);

        return new GradeSummaryResponse(
                period.response(),
                overallAverage,
                rankLabel(overallAverage),
                subjectSummaries.size(),
                (int) subjectSummaries.stream().filter(subject -> isExcellent(subject.average())).count(),
                strongestSubject,
                subjectSummaries);
    }

    @Transactional(readOnly = true)
    public GradeSubjectDetailResponse getGradeSubjectDetail(UUID studentId, String subjectId, String periodId) {
        UserEntity student = requireUser(studentId);
        SubjectEntity subject = resolveSubject(subjectId);
        PeriodResolution period = resolvePeriod(periodId);
        List<UUID> semesterIds = period.semesters().stream().map(SemesterEntity::getId).toList();
        List<GradeEntity> grades = semesterIds.isEmpty()
                ? List.of()
                : gradeRepository.findByUserIdAndSubjectIdAndSemesterIdInOrderByAssessmentDateAsc(
                        student.getId(), subject.getId(), semesterIds);
        Map<UUID, String> teacherNames = teacherNameMap(student, period.semesters());

        return new GradeSubjectDetailResponse(
                period.response(),
                toGradeSummarySubject(subject.getId(), grades, subject, teacherNames),
                toComponentScores(grades));
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
        return findCurrentSemesterEntity().orElseThrow(() -> notFound("Current semester was not found"));
    }

    private Optional<SemesterEntity> findCurrentSemesterEntity() {
        return schoolYearRepository.findByCurrentTrue()
                .flatMap(schoolYear -> semesterRepository.findBySchoolYearIdAndCurrentTrue(schoolYear.getId()));
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

    private SchoolYearEntity resolveAcademicPeriodSchoolYear(UserEntity student) {
        return findClass(student.getClassId())
                .flatMap(clazz -> schoolYearRepository.findById(clazz.getSchoolYearId()))
                .orElseGet(this::getCurrentSchoolYearEntity);
    }

    private AcademicPeriodResponse toAcademicPeriod(SemesterEntity semester, SchoolYearEntity schoolYear) {
        return new AcademicPeriodResponse(
                "semester_" + semester.getSemesterNo() + "_" + schoolYearCode(schoolYear),
                semesterLabel(semester.getSemesterNo()),
                semesterTitle(semester.getSemesterNo()),
                formatSchoolYearLabel(schoolYear),
                "semester",
                semester.getSemesterNo());
    }

    private AcademicPeriodResponse toAcademicYearPeriod(SchoolYearEntity schoolYear, int order) {
        return new AcademicPeriodResponse(
                "year_" + schoolYearCode(schoolYear),
                "C\u1EA3 n\u0103m",
                "T\u1ED5ng k\u1EBFt c\u1EA3 n\u0103m",
                formatSchoolYearLabel(schoolYear),
                "year",
                order);
    }

    private String semesterLabel(Short semesterNo) {
        return "HK " + semesterRomanNumeral(semesterNo);
    }

    private String semesterTitle(Short semesterNo) {
        return "H\u1ECDc k\u1EF3 " + semesterRomanNumeral(semesterNo);
    }

    private String semesterRomanNumeral(Short semesterNo) {
        if (semesterNo == null) {
            return "";
        }
        if (semesterNo == 1) {
            return "I";
        }
        if (semesterNo == 2) {
            return "II";
        }
        return semesterNo.toString();
    }

    private String formatSchoolYearLabel(SchoolYearEntity schoolYear) {
        if (schoolYear.getStartDate() != null && schoolYear.getEndDate() != null) {
            return schoolYear.getStartDate().getYear() + " - " + schoolYear.getEndDate().getYear();
        }
        return schoolYear.getName();
    }

    private String schoolYearCode(SchoolYearEntity schoolYear) {
        if (schoolYear.getStartDate() != null) {
            return String.valueOf(schoolYear.getStartDate().getYear());
        }
        if (schoolYear.getName() != null) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d{4}").matcher(schoolYear.getName());
            if (matcher.find()) {
                return matcher.group();
            }
        }
        return schoolYear.getId().toString();
    }

    private PeriodResolution resolvePeriod(String periodId) {
        if (periodId == null || periodId.isBlank()) {
            SemesterEntity semester = getCurrentSemesterEntity();
            return toSemesterPeriodResolution(semester, requireSchoolYear(semester.getSchoolYearId()));
        }
        if (periodId.startsWith("semester_")) {
            String[] parts = periodId.split("_", -1);
            if (parts.length != 3 || parts[1].isBlank() || parts[2].isBlank()) {
                throw badRequest("Invalid period id");
            }
            Short semesterNo = parseSemesterNo(parts[1]);
            SchoolYearEntity schoolYear = requireSchoolYearByCode(parts[2]);
            SemesterEntity semester = semesterRepository.findBySchoolYearId(schoolYear.getId()).stream()
                    .filter(item -> semesterNo.equals(item.getSemesterNo()))
                    .findFirst()
                    .orElseThrow(() -> notFound("Academic period was not found"));
            return toSemesterPeriodResolution(semester, schoolYear);
        }
        if (periodId.startsWith("year_")) {
            String yearCode = periodId.substring("year_".length());
            if (yearCode.isBlank()) {
                throw badRequest("Invalid period id");
            }
            SchoolYearEntity schoolYear = requireSchoolYearByCode(yearCode);
            List<SemesterEntity> semesters = semesterRepository.findBySchoolYearId(schoolYear.getId()).stream()
                    .sorted(Comparator.comparing(SemesterEntity::getSemesterNo)
                            .thenComparing(SemesterEntity::getStartDate))
                    .toList();
            return new PeriodResolution(
                    new GradeSummaryResponse.Period(
                            "year_" + schoolYearCode(schoolYear),
                            "C\u1EA3 n\u0103m",
                            "T\u1ED5ng k\u1EBFt c\u1EA3 n\u0103m",
                            formatSchoolYearLabel(schoolYear)),
                    semesters);
        }
        throw badRequest("Invalid period id");
    }

    private PeriodResolution toSemesterPeriodResolution(SemesterEntity semester, SchoolYearEntity schoolYear) {
        return new PeriodResolution(
                new GradeSummaryResponse.Period(
                        "semester_" + semester.getSemesterNo() + "_" + schoolYearCode(schoolYear),
                        semesterLabel(semester.getSemesterNo()),
                        semesterTitle(semester.getSemesterNo()),
                        formatSchoolYearLabel(schoolYear)),
                List.of(semester));
    }

    private SchoolYearEntity requireSchoolYear(UUID schoolYearId) {
        return schoolYearRepository.findById(schoolYearId)
                .orElseThrow(() -> notFound("School year was not found"));
    }

    private SchoolYearEntity requireSchoolYearByCode(String code) {
        String normalizedCode = code.trim();
        return schoolYearRepository.findAll().stream()
                .filter(schoolYear -> normalizedCode.equals(schoolYearCode(schoolYear)))
                .findFirst()
                .orElseThrow(() -> notFound("Academic period was not found"));
    }

    private Short parseSemesterNo(String value) {
        try {
            return Short.valueOf(value);
        } catch (NumberFormatException exception) {
            throw badRequest("Invalid period id");
        }
    }

    private SubjectEntity resolveSubject(String subjectId) {
        if (subjectId == null || subjectId.isBlank()) {
            throw badRequest("Subject id is required");
        }
        try {
            return subjectRepository.findById(UUID.fromString(subjectId))
                    .orElseThrow(() -> notFound("Subject was not found"));
        } catch (IllegalArgumentException exception) {
            return subjectRepository.findByCode(subjectId.toUpperCase(Locale.ROOT))
                    .orElseThrow(() -> notFound("Subject was not found"));
        }
    }

    private Map<UUID, String> teacherNameMap(UserEntity student, List<SemesterEntity> semesters) {
        Optional<ClassEntity> clazz = findClass(student.getClassId());
        if (clazz.isEmpty()) {
            return Map.of();
        }
        return semesters.stream()
                .flatMap(semester -> timetableRepository
                        .findByClassIdAndSemesterIdOrderByDayOfWeekAscPeriodNoAsc(
                                clazz.get().getId(), semester.getId())
                        .stream())
                .filter(entry -> entry.getSubjectId() != null)
                .filter(entry -> entry.getTeacherName() != null && !entry.getTeacherName().isBlank())
                .collect(Collectors.toMap(
                        TimetableEntryEntity::getSubjectId,
                        TimetableEntryEntity::getTeacherName,
                        (first, ignored) -> first,
                        LinkedHashMap::new));
    }

    private List<GradeSubjectDetailResponse.ComponentScore> toComponentScores(List<GradeEntity> grades) {
        Map<String, Integer> counters = new LinkedHashMap<>();
        return grades.stream()
                .map(grade -> toComponentScore(grade, counters))
                .toList();
    }

    private GradeSubjectDetailResponse.ComponentScore toComponentScore(
            GradeEntity grade, Map<String, Integer> counters) {
        return new GradeSubjectDetailResponse.ComponentScore(
                componentScoreId(grade, counters),
                grade.getTitle(),
                normalizedScoreOnTen(grade),
                normalizedCoefficient(grade.getWeight()),
                grade.getAssessmentDate());
    }

    private String componentScoreId(GradeEntity grade, Map<String, Integer> counters) {
        if (grade.getComponentCode() != null && !grade.getComponentCode().isBlank()) {
            return grade.getComponentCode();
        }
        String base = componentScoreBaseId(grade);
        int index = counters.merge(base, 1, Integer::sum);
        if ("final_exam".equals(base)) {
            return index == 1 ? base : base + "_" + index;
        }
        return base + "_" + index;
    }

    private String componentScoreBaseId(GradeEntity grade) {
        String normalizedTitle = normalizeText(grade.getTitle());
        if (normalizedTitle.contains("mieng")) {
            return "oral";
        }
        if (normalizedTitle.contains("15")) {
            return "quiz_15m";
        }
        if (normalizedTitle.contains("1 tiet") || normalizedTitle.contains("mot tiet")) {
            return "period_test";
        }
        if (grade.getGradeType() == GradeType.FINAL || normalizedTitle.contains("thi")) {
            return "final_exam";
        }
        if (grade.getGradeType() == null) {
            return "grade";
        }
        return grade.getGradeType().name().toLowerCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private GradeSummaryResponse.SubjectSummary toGradeSummarySubject(
            UUID subjectId,
            List<GradeEntity> grades,
            SubjectEntity subject,
            Map<UUID, String> teacherNames) {
        BigDecimal average = weightedAverageOnTen(grades);
        String subjectCode = subject == null ? null : subject.getCode();
        return new GradeSummaryResponse.SubjectSummary(
                subjectClientId(subjectId, subjectCode),
                subject == null ? null : subject.getName(),
                teacherNames.get(subjectId),
                subjectGroup(subject),
                average,
                rankLabel(average),
                subjectAccentColor(subject));
    }

    private String subjectClientId(UUID subjectId, String subjectCode) {
        if (subjectCode != null && !subjectCode.isBlank()) {
            return subjectCode.toLowerCase(Locale.ROOT);
        }
        return subjectId.toString();
    }

    private BigDecimal weightedAverageOnTen(List<GradeEntity> grades) {
        BigDecimal weightedTotal = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (GradeEntity grade : grades) {
            if (grade.getScore() == null) {
                continue;
            }
            BigDecimal maxScore = grade.getMaxScore() == null || grade.getMaxScore().compareTo(BigDecimal.ZERO) <= 0
                    ? BigDecimal.TEN
                    : grade.getMaxScore();
            BigDecimal weight = grade.getWeight() == null || grade.getWeight().compareTo(BigDecimal.ZERO) <= 0
                    ? BigDecimal.ONE
                    : grade.getWeight();
            BigDecimal normalizedScore = grade.getScore()
                    .multiply(BigDecimal.TEN)
                    .divide(maxScore, 4, RoundingMode.HALF_UP);
            weightedTotal = weightedTotal.add(normalizedScore.multiply(weight));
            totalWeight = totalWeight.add(weight);
        }
        if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return weightedTotal.divide(totalWeight, 1, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizedScoreOnTen(GradeEntity grade) {
        if (grade.getScore() == null) {
            return null;
        }
        BigDecimal maxScore = grade.getMaxScore() == null || grade.getMaxScore().compareTo(BigDecimal.ZERO) <= 0
                ? BigDecimal.TEN
                : grade.getMaxScore();
        return grade.getScore()
                .multiply(BigDecimal.TEN)
                .divide(maxScore, 1, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizedCoefficient(BigDecimal coefficient) {
        BigDecimal resolvedCoefficient = coefficient == null || coefficient.compareTo(BigDecimal.ZERO) <= 0
                ? BigDecimal.ONE
                : coefficient;
        return resolvedCoefficient.stripTrailingZeros();
    }

    private BigDecimal averageSubjectScores(List<GradeSummaryResponse.SubjectSummary> subjects) {
        if (subjects.isEmpty()) {
            return null;
        }
        BigDecimal total = subjects.stream()
                .map(GradeSummaryResponse.SubjectSummary::average)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(subjects.size()), 1, RoundingMode.HALF_UP);
    }

    private String rankLabel(BigDecimal average) {
        if (average == null) {
            return null;
        }
        if (average.compareTo(BigDecimal.valueOf(8)) >= 0) {
            return "Gi\u1ECFi";
        }
        if (average.compareTo(BigDecimal.valueOf(6.5)) >= 0) {
            return "Kh\u00E1";
        }
        if (average.compareTo(BigDecimal.valueOf(5)) >= 0) {
            return "Trung b\u00ECnh";
        }
        return "Y\u1EBFu";
    }

    private boolean isExcellent(BigDecimal average) {
        return average != null && average.compareTo(BigDecimal.valueOf(8)) >= 0;
    }

    private String subjectGroup(SubjectEntity subject) {
        if (subject != null && subject.getSubjectGroup() != null && !subject.getSubjectGroup().isBlank()) {
            return subjectGroupLabel(subject.getSubjectGroup());
        }
        return subjectGroupLabel(fallbackSubjectGroupCode(subject == null ? null : subject.getCode()));
    }

    private String subjectGroupLabel(String subjectGroup) {
        String group = subjectGroup == null ? "" : subjectGroup.toUpperCase(Locale.ROOT);
        return switch (group) {
            case "NATURAL" -> "T\u1EF1 nhi\u00EAn";
            case "SOCIAL" -> "X\u00E3 h\u1ED9i";
            case "FOREIGN_LANGUAGE" -> "Ngo\u1EA1i ng\u1EEF";
            case "SKILL" -> "K\u1EF9 n\u0103ng";
            default -> "Kh\u00E1c";
        };
    }

    private String fallbackSubjectGroupCode(String subjectCode) {
        String code = normalizeSubjectCode(subjectCode);
        return switch (code) {
            case "MATH", "PHYSICS", "CHEMISTRY", "BIOLOGY", "INFORMATICS", "TECHNOLOGY" -> "NATURAL";
            case "LITERATURE", "HISTORY", "GEOGRAPHY", "CIVICS", "LOCAL_EDU" -> "SOCIAL";
            case "ENGLISH" -> "FOREIGN_LANGUAGE";
            case "PE", "DEFENSE" -> "SKILL";
            default -> "OTHER";
        };
    }

    private String subjectAccentColor(SubjectEntity subject) {
        if (subject != null && subject.getAccentColor() != null && !subject.getAccentColor().isBlank()) {
            return subject.getAccentColor();
        }
        String code = normalizeSubjectCode(subject == null ? null : subject.getCode());
        return switch (code) {
            case "MATH" -> "#F46A00";
            case "LITERATURE" -> "#B07D56";
            case "ENGLISH" -> "#2563EB";
            case "PHYSICS" -> "#0EA5E9";
            case "CHEMISTRY" -> "#16A34A";
            case "BIOLOGY" -> "#65A30D";
            case "INFORMATICS" -> "#7C3AED";
            case "HISTORY" -> "#B45309";
            case "GEOGRAPHY" -> "#0891B2";
            case "CIVICS" -> "#DB2777";
            case "TECHNOLOGY" -> "#475569";
            case "PE" -> "#DC2626";
            case "DEFENSE" -> "#4B5563";
            case "LOCAL_EDU" -> "#059669";
            default -> "#64748B";
        };
    }

    private int subjectDisplayOrder(String subjectId) {
        String normalizedSubjectId = subjectId == null ? "" : subjectId.toLowerCase(Locale.ROOT);
        return switch (normalizedSubjectId) {
            case "math" -> 1;
            case "literature" -> 2;
            case "english" -> 3;
            case "physics" -> 4;
            case "chemistry" -> 5;
            case "biology" -> 6;
            case "informatics" -> 7;
            case "history" -> 8;
            case "geography" -> 9;
            case "civics" -> 10;
            case "technology" -> 11;
            case "pe" -> 12;
            case "defense" -> 13;
            case "local_edu" -> 14;
            default -> 999;
        };
    }

    private String normalizeSubjectCode(String subjectCode) {
        return subjectCode == null ? "" : subjectCode.toUpperCase(Locale.ROOT);
    }

    private record PeriodResolution(GradeSummaryResponse.Period response, List<SemesterEntity> semesters) {
    }

    private StudentDashboardResponse.ScheduleItem toDashboardScheduleItem(
            TimetableEntryEntity entry, SubjectEntity subject, LocalTime now) {
        String status = lessonStatus(entry, now);
        return new StudentDashboardResponse.ScheduleItem(
                timeLabel(entry.getStartTime(), entry.getEndTime()),
                subject == null ? null : subject.getName(),
                periodLabel(entry.getPeriodNo()),
                entry.getRoomName(),
                entry.getTeacherName(),
                status,
                lessonStatusLabel(status));
    }

    private StudentDashboardResponse.CurrentLesson toDashboardCurrentLesson(
            StudentDashboardResponse.ScheduleItem item) {
        return new StudentDashboardResponse.CurrentLesson(
                item.subjectName(),
                item.periodLabel(),
                item.roomName(),
                item.teacherName(),
                item.statusLabel());
    }

    private StudentDashboardResponse.RecentGrade toDashboardGrade(GradeEntity grade, SubjectEntity subject) {
        return new StudentDashboardResponse.RecentGrade(
                subject == null ? null : subject.getName(),
                grade.getScore(),
                grade.getMaxScore(),
                gradeLabel(grade.getScore(), grade.getMaxScore()));
    }

    private String todayTitle(LocalDate date) {
        return "Hôm nay · " + vietnameseDayName(date.getDayOfWeek());
    }

    private String vietnameseDayName(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "Thứ hai";
            case TUESDAY -> "Thứ ba";
            case WEDNESDAY -> "Thứ tư";
            case THURSDAY -> "Thứ năm";
            case FRIDAY -> "Thứ sáu";
            case SATURDAY -> "Thứ bảy";
            case SUNDAY -> "Chủ nhật";
        };
    }

    private String timeLabel(LocalTime startTime, LocalTime endTime) {
        return startTime.format(LESSON_TIME_FORMATTER) + "-" + endTime.format(LESSON_TIME_FORMATTER);
    }

    private String periodLabel(Short periodNo) {
        return "Tiết " + periodNo;
    }

    private String lessonStatus(TimetableEntryEntity entry, LocalTime now) {
        if (now.isBefore(entry.getStartTime())) {
            return LESSON_STATUS_UPCOMING;
        }
        if (now.isBefore(entry.getEndTime())) {
            return LESSON_STATUS_LIVE;
        }
        return LESSON_STATUS_DONE;
    }

    private String lessonStatusLabel(String status) {
        return switch (status) {
            case LESSON_STATUS_DONE -> "Đã xong";
            case LESSON_STATUS_LIVE -> "Đang học";
            case LESSON_STATUS_UPCOMING -> "Sắp học";
            default -> status;
        };
    }

    private String gradeLabel(BigDecimal score, BigDecimal maxScore) {
        if (score == null) {
            return "Chưa có điểm";
        }
        BigDecimal normalizedScore = score.multiply(BigDecimal.TEN)
                .divide(maxScore == null ? BigDecimal.TEN : maxScore, 2, RoundingMode.HALF_UP);
        if (normalizedScore.compareTo(BigDecimal.valueOf(8)) >= 0) {
            return "Tốt";
        }
        if (normalizedScore.compareTo(BigDecimal.valueOf(6.5)) >= 0) {
            return "Khá";
        }
        if (normalizedScore.compareTo(BigDecimal.valueOf(5)) >= 0) {
            return "Đạt";
        }
        return "Cần cải thiện";
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

    private ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}
