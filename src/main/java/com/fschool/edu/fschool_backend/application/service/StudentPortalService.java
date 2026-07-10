package com.fschool.edu.fschool_backend.application.service;

import com.fschool.edu.fschool_backend.domain.enums.ExamType;
import com.fschool.edu.fschool_backend.domain.enums.GradeType;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.ClassEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.ExamEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.GradeEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.NotificationEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.SchoolYearEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.SemesterEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.SubjectEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.TimetableEntryEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.UserEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.ClassJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.ExamJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.GradeJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.NotificationJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.SchoolYearJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.SemesterJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.SubjectJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.TimetableEntryJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.UserJpaRepository;
import com.fschool.edu.fschool_backend.presentation.dto.response.AcademicPeriodResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.GradeSubjectDetailResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.GradeSummaryResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentDashboardResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentExamScheduleResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentNotificationsResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentTimetableResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentTuitionResponse;
import com.fschool.edu.fschool_backend.presentation.exception.ApiException;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentPortalService {

    private static final DateTimeFormatter LESSON_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter EXAM_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final String LESSON_STATUS_DONE = "done";
    private static final String LESSON_STATUS_LIVE = "live";
    private static final String LESSON_STATUS_NEXT = "next";
    private static final String LESSON_STATUS_NORMAL = "normal";
    private static final String LESSON_STATUS_UPCOMING = "upcoming";
    private static final ZoneId DASHBOARD_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final UserJpaRepository userRepository;
    private final ClassJpaRepository classRepository;
    private final SchoolYearJpaRepository schoolYearRepository;
    private final SemesterJpaRepository semesterRepository;
    private final SubjectJpaRepository subjectRepository;
    private final TimetableEntryJpaRepository timetableRepository;
    private final GradeJpaRepository gradeRepository;
    private final ExamJpaRepository examRepository;
    private final NotificationJpaRepository notificationRepository;

    public StudentPortalService(
            UserJpaRepository userRepository,
            ClassJpaRepository classRepository,
            SchoolYearJpaRepository schoolYearRepository,
            SemesterJpaRepository semesterRepository,
            SubjectJpaRepository subjectRepository,
            TimetableEntryJpaRepository timetableRepository,
            GradeJpaRepository gradeRepository,
            ExamJpaRepository examRepository,
            NotificationJpaRepository notificationRepository) {
        this.userRepository = userRepository;
        this.classRepository = classRepository;
        this.schoolYearRepository = schoolYearRepository;
        this.semesterRepository = semesterRepository;
        this.subjectRepository = subjectRepository;
        this.timetableRepository = timetableRepository;
        this.gradeRepository = gradeRepository;
        this.examRepository = examRepository;
        this.notificationRepository = notificationRepository;
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
    public StudentTuitionResponse getStudentTuition(UUID studentId) {
        UserEntity student = requireUser(studentId);
        ClassEntity clazz = requireClass(student);
        SemesterEntity semester = findCurrentSemesterForClass(clazz)
                .orElseThrow(() -> notFound("Current semester was not found"));
        SchoolYearEntity schoolYear = requireSchoolYear(semester.getSchoolYearId());

        return new StudentTuitionResponse(
                tuitionSemesterName(semester, schoolYear),
                new StudentTuitionResponse.Student(student.getFullName(), clazz.getName()),
                0L,
                0L,
                0L,
                null,
                0,
                null,
                null,
                List.of(),
                List.of());
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
    public StudentTimetableResponse getStudentTimetable(UUID userId, LocalDate startDate, LocalDate endDate) {
        UserEntity user = requireUser(userId);
        ClassEntity clazz = requireClass(user);
        LocalDate resolvedStart = startDate == null
                ? LocalDate.now(DASHBOARD_ZONE).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                : startDate;
        LocalDate resolvedEnd = endDate == null ? resolvedStart.plusDays(6) : endDate;
        if (resolvedEnd.isBefore(resolvedStart)) {
            resolvedEnd = resolvedStart;
        }

        Map<UUID, SubjectEntity> subjects = subjectMap();
        List<StudentTimetableResponse.Day> days = new ArrayList<>();
        LocalDate currentDate = resolvedStart;
        while (!currentDate.isAfter(resolvedEnd)) {
            days.add(toStudentTimetableDay(clazz, currentDate, subjects));
            currentDate = currentDate.plusDays(1);
        }
        return new StudentTimetableResponse(resolvedStart, resolvedEnd, days);
    }

    @Transactional(readOnly = true)
    public StudentExamScheduleResponse getStudentExamSchedule(UUID studentId) {
        UserEntity student = requireUser(studentId);
        ClassEntity clazz = requireClass(student);
        Map<UUID, SubjectEntity> subjects = subjectMap();
        Optional<SemesterEntity> semester = findCurrentSemesterForClass(clazz);
        List<ExamEntity> exams = semester
                .map(value -> examRepository.findByClassIdAndSemesterIdOrderByExamDateAscStartTimeAsc(
                        clazz.getId(), value.getId()))
                .orElseGet(List::of);
        LocalDate today = LocalDate.now(DASHBOARD_ZONE);
        List<StudentExamScheduleResponse.ExamItem> examItems = exams.stream()
                .map(exam -> toStudentExamItem(exam, subjects.get(exam.getSubjectId()), today))
                .toList();
        Instant lastUpdatedAt = exams.stream()
                .map(ExamEntity::getUpdatedAt)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElseGet(Instant::now);

        return new StudentExamScheduleResponse(
                semester.map(this::examTermName).orElse(null),
                lastUpdatedAt,
                examItems);
    }

    @Transactional(readOnly = true)
    public StudentNotificationsResponse getStudentNotifications(UUID studentId) {
        UserEntity student = requireUser(studentId);
        List<NotificationEntity> notifications =
                notificationRepository.findByUserIdOrderByCreatedAtDesc(student.getId());
        long unreadCount = notificationRepository.countByUserIdAndReadFalse(student.getId());
        return new StudentNotificationsResponse(
                unreadCount,
                notifications.stream()
                        .map(this::toStudentNotificationItem)
                        .toList());
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

    private Optional<SemesterEntity> findCurrentSemesterEntity() {
        return schoolYearRepository.findByCurrentTrue()
                .flatMap(schoolYear -> semesterRepository.findBySchoolYearIdAndCurrentTrue(schoolYear.getId()));
    }

    private Optional<SemesterEntity> findCurrentSemesterForClass(ClassEntity clazz) {
        return semesterRepository.findBySchoolYearId(clazz.getSchoolYearId()).stream()
                .filter(semester -> Boolean.TRUE.equals(semester.getCurrent()))
                .findFirst()
                .or(this::findCurrentSemesterEntity);
    }

    private SchoolYearEntity getCurrentSchoolYearEntity() {
        return schoolYearRepository.findByCurrentTrue().orElseThrow(() -> notFound("Current school year was not found"));
    }

    private Map<UUID, SubjectEntity> subjectMap() {
        return subjectRepository.findAll().stream().collect(Collectors.toMap(SubjectEntity::getId, Function.identity()));
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

    private String tuitionSemesterName(SemesterEntity semester, SchoolYearEntity schoolYear) {
        return semesterTitle(semester.getSemesterNo()) + " " + formatSchoolYearLabel(schoolYear);
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
            SemesterEntity semester = findCurrentSemesterEntity()
                    .orElseThrow(() -> notFound("Current semester was not found"));
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
                rankLabel(average));
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

    private StudentTimetableResponse.Day toStudentTimetableDay(
            ClassEntity clazz,
            LocalDate date,
            Map<UUID, SubjectEntity> subjects) {
        short dayOfWeek = (short) date.getDayOfWeek().getValue();
        List<TimetableEntryEntity> entries = findTimetableSemester(clazz, date)
                .map(semester -> timetableRepository
                        .findByClassIdAndSemesterIdAndDayOfWeekOrderByPeriodNoAsc(
                                clazz.getId(), semester.getId(), dayOfWeek))
                .orElseGet(List::of);
        UUID nextLessonId = nextStudentTimetableLessonId(date, entries).orElse(null);
        List<StudentTimetableResponse.Lesson> lessons = entries.stream()
                .map(entry -> toStudentTimetableLesson(
                        entry,
                        subjects.get(entry.getSubjectId()),
                        clazz,
                        date,
                        nextLessonId))
                .toList();
        return new StudentTimetableResponse.Day(
                date,
                studentTimetableDayLabel(date.getDayOfWeek()),
                lessons);
    }

    private Optional<SemesterEntity> findTimetableSemester(ClassEntity clazz, LocalDate date) {
        List<SemesterEntity> classSemesters = semesterRepository.findBySchoolYearId(clazz.getSchoolYearId());
        return classSemesters.stream()
                .filter(semester -> !date.isBefore(semester.getStartDate()) && !date.isAfter(semester.getEndDate()))
                .findFirst()
                .or(() -> classSemesters.stream()
                        .filter(semester -> Boolean.TRUE.equals(semester.getCurrent()))
                        .findFirst())
                .or(this::findCurrentSemesterEntity);
    }

    private Optional<UUID> nextStudentTimetableLessonId(LocalDate date, List<TimetableEntryEntity> entries) {
        LocalDate today = LocalDate.now(DASHBOARD_ZONE);
        if (!date.equals(today)) {
            return Optional.empty();
        }
        LocalTime now = LocalTime.now(DASHBOARD_ZONE);
        return entries.stream()
                .filter(entry -> now.isBefore(entry.getStartTime()))
                .findFirst()
                .map(TimetableEntryEntity::getId);
    }

    private StudentTimetableResponse.Lesson toStudentTimetableLesson(
            TimetableEntryEntity entry,
            SubjectEntity subject,
            ClassEntity clazz,
            LocalDate date,
            UUID nextLessonId) {
        String status = studentTimetableLessonStatus(entry, date, nextLessonId);
        return new StudentTimetableResponse.Lesson(
                entry.getId().toString(),
                subject == null ? null : subject.getName(),
                clazz.getName(),
                entry.getRoomName(),
                entry.getTeacherName(),
                entry.getPeriodNo(),
                studentTimetablePeriodLabel(entry.getPeriodNo()),
                entry.getStartTime().format(LESSON_TIME_FORMATTER),
                entry.getEndTime().format(LESSON_TIME_FORMATTER),
                status,
                studentTimetableStatusLabel(status),
                "");
    }

    private StudentExamScheduleResponse.ExamItem toStudentExamItem(
            ExamEntity exam,
            SubjectEntity subject,
            LocalDate today) {
        String status = examStatus(exam.getExamDate(), today);
        return new StudentExamScheduleResponse.ExamItem(
                exam.getId().toString(),
                subject == null ? exam.getTitle() : subject.getName(),
                examTypeLabel(exam.getExamType()),
                exam.getExamDate(),
                formatExamTime(exam.getStartTime()),
                formatExamTime(exam.getStartTime().plusMinutes(exam.getDurationMinutes())),
                exam.getDurationMinutes(),
                exam.getRoomName(),
                null,
                null,
                status,
                examStatusLabel(status),
                exam.getNote());
    }

    private String examTermName(SemesterEntity semester) {
        SchoolYearEntity schoolYear = requireSchoolYear(semester.getSchoolYearId());
        return "H\u1ECDc k\u1EF3 " + semester.getSemesterNo()
                + " - N\u0103m h\u1ECDc " + formatSchoolYearLabel(schoolYear);
    }

    private String formatExamTime(LocalTime time) {
        return time == null ? null : time.format(EXAM_TIME_FORMATTER);
    }

    private String examTypeLabel(ExamType examType) {
        return switch (examType) {
            case MIDTERM -> "Gi\u1EEFa k\u1EF3";
            case FINAL -> "Cu\u1ED1i k\u1EF3";
            case OTHER -> "Kh\u00E1c";
        };
    }

    private String examStatus(LocalDate examDate, LocalDate today) {
        if (examDate.isBefore(today)) {
            return "finished";
        }
        if (examDate.isEqual(today)) {
            return "today";
        }
        return "upcoming";
    }

    private String examStatusLabel(String status) {
        return switch (status) {
            case "finished" -> "\u0110\u00E3 thi";
            case "today" -> "Thi h\u00F4m nay";
            case "upcoming" -> "S\u1EAFp thi";
            case "cancelled" -> "\u0110\u00E3 h\u1EE7y";
            default -> status;
        };
    }

    private StudentNotificationsResponse.NotificationItem toStudentNotificationItem(NotificationEntity notification) {
        String notificationType = normalizeNotificationType(notification.getNotificationType());
        String deepLink = normalizeNotificationDeepLink(notification.getDeepLink());
        return new StudentNotificationsResponse.NotificationItem(
                notification.getId().toString(),
                notification.getTitle(),
                notification.getBody(),
                notificationCategory(notificationType),
                notification.getCreatedAt(),
                Boolean.TRUE.equals(notification.getRead()),
                notificationActionLabel(notificationType, deepLink),
                deepLink);
    }

    private String normalizeNotificationType(String notificationType) {
        return notificationType == null ? "" : notificationType.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeNotificationDeepLink(String deepLink) {
        return deepLink == null || deepLink.isBlank() ? "" : deepLink.trim();
    }

    private String notificationCategory(String notificationType) {
        return switch (notificationType) {
            case "ACADEMIC", "GRADE", "SCORE", "EXAM", "ASSIGNMENT", "TIMETABLE", "SCHEDULE" -> "academic";
            case "TUITION", "FEE", "PAYMENT" -> "tuition";
            case "REQUEST", "FORM", "APPLICATION" -> "request";
            case "SYSTEM", "ACCOUNT", "SECURITY" -> "system";
            case "EVENT", "NEWS", "ANNOUNCEMENT" -> "event";
            default -> "general";
        };
    }

    private String notificationActionLabel(String notificationType, String deepLink) {
        if (deepLink == null || deepLink.isBlank()) {
            return "";
        }
        return switch (notificationType) {
            case "GRADE", "SCORE" -> "Xem \u0111i\u1EC3m";
            case "EXAM" -> "Xem l\u1ECBch thi";
            case "ASSIGNMENT" -> "Xem b\u00E0i t\u1EADp";
            case "TIMETABLE", "SCHEDULE" -> "Xem th\u1EDDi kh\u00F3a bi\u1EC3u";
            case "TUITION", "FEE", "PAYMENT" -> "Xem h\u1ECDc ph\u00ED";
            case "REQUEST", "FORM", "APPLICATION" -> "Xem \u0111\u01A1n";
            default -> "Xem chi ti\u1EBFt";
        };
    }

    private String studentTimetableLessonStatus(TimetableEntryEntity entry, LocalDate date, UUID nextLessonId) {
        LocalDate today = LocalDate.now(DASHBOARD_ZONE);
        if (date.isBefore(today)) {
            return LESSON_STATUS_DONE;
        }
        if (date.isAfter(today)) {
            return LESSON_STATUS_NORMAL;
        }
        LocalTime now = LocalTime.now(DASHBOARD_ZONE);
        if (!now.isBefore(entry.getStartTime()) && now.isBefore(entry.getEndTime())) {
            return LESSON_STATUS_LIVE;
        }
        if (!now.isBefore(entry.getEndTime())) {
            return LESSON_STATUS_DONE;
        }
        if (nextLessonId != null && nextLessonId.equals(entry.getId())) {
            return LESSON_STATUS_NEXT;
        }
        return LESSON_STATUS_NORMAL;
    }

    private String studentTimetableDayLabel(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "Th\u1EE9 2";
            case TUESDAY -> "Th\u1EE9 3";
            case WEDNESDAY -> "Th\u1EE9 4";
            case THURSDAY -> "Th\u1EE9 5";
            case FRIDAY -> "Th\u1EE9 6";
            case SATURDAY -> "Th\u1EE9 7";
            case SUNDAY -> "Ch\u1EE7 nh\u1EADt";
        };
    }

    private String studentTimetablePeriodLabel(Short periodNo) {
        return "Ti\u1EBFt " + periodNo;
    }

    private String studentTimetableStatusLabel(String status) {
        return switch (status) {
            case LESSON_STATUS_DONE -> "\u0110\u00E3 h\u1ECDc";
            case LESSON_STATUS_LIVE -> "\u0110ang h\u1ECDc";
            case LESSON_STATUS_NEXT -> "S\u1EAFp t\u1EDBi";
            case LESSON_STATUS_NORMAL -> "B\u00ECnh th\u01B0\u1EDDng";
            default -> status;
        };
    }

    private StudentDashboardResponse.ScheduleItem toDashboardScheduleItem(
            TimetableEntryEntity entry, SubjectEntity subject, LocalTime now) {
        String status = dashboardLessonStatus(entry, now);
        return new StudentDashboardResponse.ScheduleItem(
                timeLabel(entry.getStartTime(), entry.getEndTime()),
                subject == null ? null : subject.getName(),
                periodLabel(entry.getPeriodNo()),
                entry.getRoomName(),
                entry.getTeacherName(),
                status,
                dashboardLessonStatusLabel(status));
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
        return "H\u00F4m nay - " + vietnameseDayName(date.getDayOfWeek());
    }

    private String vietnameseDayName(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "Th\u1EE9 hai";
            case TUESDAY -> "Th\u1EE9 ba";
            case WEDNESDAY -> "Th\u1EE9 t\u01B0";
            case THURSDAY -> "Th\u1EE9 n\u0103m";
            case FRIDAY -> "Th\u1EE9 s\u00E1u";
            case SATURDAY -> "Th\u1EE9 b\u1EA3y";
            case SUNDAY -> "Ch\u1EE7 nh\u1EADt";
        };
    }

    private String timeLabel(LocalTime startTime, LocalTime endTime) {
        return startTime.format(LESSON_TIME_FORMATTER) + "-" + endTime.format(LESSON_TIME_FORMATTER);
    }

    private String periodLabel(Short periodNo) {
        return "Ti\u1EBFt " + periodNo;
    }

    private String dashboardLessonStatus(TimetableEntryEntity entry, LocalTime now) {
        if (now.isBefore(entry.getStartTime())) {
            return LESSON_STATUS_UPCOMING;
        }
        if (now.isBefore(entry.getEndTime())) {
            return LESSON_STATUS_LIVE;
        }
        return LESSON_STATUS_DONE;
    }

    private String dashboardLessonStatusLabel(String status) {
        return switch (status) {
            case LESSON_STATUS_DONE -> "\u0110\u00E3 xong";
            case LESSON_STATUS_LIVE -> "\u0110ang h\u1ECDc";
            case LESSON_STATUS_UPCOMING -> "S\u1EAFp h\u1ECDc";
            default -> status;
        };
    }

    private String gradeLabel(BigDecimal score, BigDecimal maxScore) {
        if (score == null) {
            return "Ch\u01B0a c\u00F3 \u0111i\u1EC3m";
        }
        BigDecimal normalizedScore = score.multiply(BigDecimal.TEN)
                .divide(maxScore == null ? BigDecimal.TEN : maxScore, 2, RoundingMode.HALF_UP);
        if (normalizedScore.compareTo(BigDecimal.valueOf(8)) >= 0) {
            return "T\u1ED1t";
        }
        if (normalizedScore.compareTo(BigDecimal.valueOf(6.5)) >= 0) {
            return "Kh\u00E1";
        }
        if (normalizedScore.compareTo(BigDecimal.valueOf(5)) >= 0) {
            return "\u0110\u1EA1t";
        }
        return "C\u1EA7n c\u1EA3i thi\u1EC7n";
    }

    private ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    private ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    private record PeriodResolution(GradeSummaryResponse.Period response, List<SemesterEntity> semesters) {
    }
}
