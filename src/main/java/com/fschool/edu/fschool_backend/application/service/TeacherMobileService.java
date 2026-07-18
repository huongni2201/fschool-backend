package com.fschool.edu.fschool_backend.application.service;

import com.fschool.edu.fschool_backend.domain.enums.Gender;
import com.fschool.edu.fschool_backend.domain.enums.UserStatus;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.ClassEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.GradeEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.NotificationEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.SchoolYearEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.SemesterEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.SubjectEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.TeacherProfileEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.TimetableEntryEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.UserEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.ClassJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.GradeJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.NotificationJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.SchoolYearJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.SemesterJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.SubjectJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.TeacherProfileJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.TimetableEntryJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.UserJpaRepository;
import com.fschool.edu.fschool_backend.presentation.dto.response.TeacherAcademicPeriodsResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.TeacherClassStudentsResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.TeacherNotificationsResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.TeacherProfileResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.TeacherStudentGradeSubjectDetailResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.TeacherStudentGradeSummaryResponse;
import com.fschool.edu.fschool_backend.presentation.exception.ApiException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeacherMobileService {

    private static final BigDecimal TEN = BigDecimal.TEN;

    private final TeacherProfileJpaRepository teacherProfileRepository;
    private final ClassJpaRepository classRepository;
    private final UserJpaRepository userRepository;
    private final SchoolYearJpaRepository schoolYearRepository;
    private final SemesterJpaRepository semesterRepository;
    private final SubjectJpaRepository subjectRepository;
    private final TimetableEntryJpaRepository timetableRepository;
    private final GradeJpaRepository gradeRepository;
    private final NotificationJpaRepository notificationRepository;
    private final String campusName;

    public TeacherMobileService(
            TeacherProfileJpaRepository teacherProfileRepository,
            ClassJpaRepository classRepository,
            UserJpaRepository userRepository,
            SchoolYearJpaRepository schoolYearRepository,
            SemesterJpaRepository semesterRepository,
            SubjectJpaRepository subjectRepository,
            TimetableEntryJpaRepository timetableRepository,
            GradeJpaRepository gradeRepository,
            NotificationJpaRepository notificationRepository,
            @Value("${app.school.campus-name:FPT Schools C\u1EA7u Gi\u1EA5y}") String campusName) {
        this.teacherProfileRepository = teacherProfileRepository;
        this.classRepository = classRepository;
        this.userRepository = userRepository;
        this.schoolYearRepository = schoolYearRepository;
        this.semesterRepository = semesterRepository;
        this.subjectRepository = subjectRepository;
        this.timetableRepository = timetableRepository;
        this.gradeRepository = gradeRepository;
        this.notificationRepository = notificationRepository;
        this.campusName = campusName;
    }

    @Transactional(readOnly = true)
    public TeacherProfileResponse getProfile(UUID teacherUserId) {
        UserEntity user = requireUser(teacherUserId);
        Optional<TeacherProfileEntity> teacherProfile = teacherProfileRepository.findByUserId(teacherUserId);
        String fullName = teacherProfile.map(TeacherProfileEntity::getFullName)
                .filter(this::hasText)
                .orElse(user.getFullName());
        String employeeCode = teacherProfile.map(TeacherProfileEntity::getEmployeeCode)
                .filter(this::hasText)
                .orElse(null);
        return new TeacherProfileResponse(
                user.getId().toString(),
                fullName,
                employeeCode,
                employeeCode,
                email(user),
                user.getPhone(),
                user.getDateOfBirth(),
                genderLabel(user.getGender()),
                user.getAddress(),
                teacherProfile.map(TeacherProfileEntity::getDepartmentName)
                        .filter(this::hasText)
                        .orElse(null),
                campusName,
                roleLabel(user, teacherProfile.orElse(null)),
                avatarText(fullName));
    }

    @Transactional(readOnly = true)
    public TeacherClassStudentsResponse getClassStudents(UUID teacherUserId, UUID classId) {
        TeacherProfileEntity teacher = requireTeacherProfile(teacherUserId);
        ClassEntity schoolClass = requireClass(classId);
        requireCanAccessClass(teacher, schoolClass);

        List<UserEntity> students = userRepository.findStudentsByClassIdAndSearch(classId, null);
        return new TeacherClassStudentsResponse(
                new TeacherClassStudentsResponse.ClassInfo(
                        schoolClass.getId().toString(),
                        schoolClass.getName(),
                        classSubjectLabel(teacher, schoolClass),
                        students.size()),
                students.stream()
                        .map(student -> new TeacherClassStudentsResponse.Student(
                                student.getId().toString(),
                                student.getStudentCode(),
                                student.getFullName(),
                                schoolClass.getName(),
                                statusLabel(student.getStatus())))
                        .toList());
    }

    @Transactional(readOnly = true)
    public TeacherAcademicPeriodsResponse getAcademicPeriods(UUID teacherUserId, UUID studentId) {
        StudentAccessContext context = requireStudentAccess(teacherUserId, studentId);
        List<SemesterEntity> semesters = semestersForClassSchoolYear(context.schoolClass());
        boolean homeroom = isHomeroomTeacher(context.teacher(), context.schoolClass());
        List<SemesterEntity> accessibleSemesters = homeroom
                ? semesters
                : semesters.stream()
                        .filter(semester -> !teacherEntriesForClassSemester(
                                context.teacher(),
                                context.schoolClass().getId(),
                                semester.getId()).isEmpty())
                        .toList();

        List<TeacherAcademicPeriodsResponse.Period> periods = accessibleSemesters.stream()
                .map(semester -> toPeriod(semester, context.schoolYear()))
                .collect(Collectors.toCollection(ArrayList::new));
        if (!periods.isEmpty()) {
            periods.add(toAcademicYearPeriod(context.schoolYear()));
        }
        return new TeacherAcademicPeriodsResponse(periods);
    }

    @Transactional(readOnly = true)
    public TeacherStudentGradeSummaryResponse getGradeSummary(
            UUID teacherUserId,
            UUID studentId,
            String periodId) {
        StudentAccessContext context = requireStudentAccess(teacherUserId, studentId);
        PeriodResolution period = resolvePeriod(periodId, context.schoolClass());
        GradeAccessScope accessScope = gradeAccessScope(context.teacher(), context.schoolClass(), period.semesters());

        List<UUID> semesterIds = period.semesters().stream().map(SemesterEntity::getId).toList();
        List<GradeEntity> grades = semesterIds.isEmpty()
                ? List.of()
                : gradeRepository.findByUserIdAndSemesterIdInOrderByAssessmentDateDesc(context.student().getId(), semesterIds)
                        .stream()
                        .filter(grade -> grade.getScore() != null)
                        .filter(accessScope::canView)
                        .toList();

        Map<UUID, SubjectEntity> subjects = subjectMap(grades.stream()
                .map(GradeEntity::getSubjectId)
                .collect(Collectors.toSet()));
        Map<UUID, String> teacherNames = teacherNameMap(context.teacher(), context.schoolClass(), period.semesters());
        Map<UUID, List<GradeEntity>> gradesBySubject = grades.stream()
                .collect(Collectors.groupingBy(GradeEntity::getSubjectId, LinkedHashMap::new, Collectors.toList()));

        List<TeacherStudentGradeSummaryResponse.SubjectGrade> subjectGrades = gradesBySubject.entrySet().stream()
                .map(entry -> toSubjectGrade(entry.getKey(), entry.getValue(), subjects.get(entry.getKey()), teacherNames))
                .filter(subject -> subject.average() != null)
                .sorted(Comparator
                        .comparingInt((TeacherStudentGradeSummaryResponse.SubjectGrade subject) ->
                                subjectDisplayOrder(subject.subjectId()))
                        .thenComparing(
                                TeacherStudentGradeSummaryResponse.SubjectGrade::subjectName,
                                Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        return new TeacherStudentGradeSummaryResponse(
                period.response(),
                averageSubjectScores(subjectGrades),
                subjectGrades);
    }

    @Transactional(readOnly = true)
    public TeacherStudentGradeSubjectDetailResponse getGradeSubjectDetail(
            UUID teacherUserId,
            UUID studentId,
            String subjectId,
            String periodId) {
        StudentAccessContext context = requireStudentAccess(teacherUserId, studentId);
        SubjectEntity subject = resolveSubject(subjectId);
        PeriodResolution period = resolvePeriod(periodId, context.schoolClass());
        GradeAccessScope accessScope = gradeAccessScope(context.teacher(), context.schoolClass(), period.semesters());
        if (!accessScope.canViewSubject(subject.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        List<UUID> semesterIds = period.semesters().stream().map(SemesterEntity::getId).toList();
        List<GradeEntity> grades = semesterIds.isEmpty()
                ? List.of()
                : gradeRepository.findByUserIdAndSubjectIdAndSemesterIdInOrderByAssessmentDateAsc(
                                context.student().getId(), subject.getId(), semesterIds)
                        .stream()
                        .filter(accessScope::canView)
                        .toList();
        Map<UUID, String> teacherNames = teacherNameMap(context.teacher(), context.schoolClass(), period.semesters());

        return new TeacherStudentGradeSubjectDetailResponse(
                subjectClientId(subject),
                subject.getName(),
                teacherNames.getOrDefault(subject.getId(), context.teacher().getFullName()),
                score(weightedAverageOnTen(grades)),
                toComponentScores(grades));
    }

    @Transactional(readOnly = true)
    public TeacherNotificationsResponse getNotifications(UUID teacherUserId) {
        requireTeacherProfile(teacherUserId);
        List<NotificationEntity> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(teacherUserId);
        return new TeacherNotificationsResponse(
                notificationRepository.countByUserIdAndReadFalse(teacherUserId),
                notifications.stream()
                        .map(this::toNotificationItem)
                        .toList());
    }

    private StudentAccessContext requireStudentAccess(UUID teacherUserId, UUID studentId) {
        TeacherProfileEntity teacher = requireTeacherProfile(teacherUserId);
        UserEntity student = requireUser(studentId);
        ClassEntity schoolClass = requireClass(student.getClassId());
        requireCanAccessClass(teacher, schoolClass);
        SchoolYearEntity schoolYear = schoolYearRepository.findById(schoolClass.getSchoolYearId())
                .orElseThrow(() -> notFound("School year was not found"));
        return new StudentAccessContext(teacher, student, schoolClass, schoolYear);
    }

    private void requireCanAccessClass(TeacherProfileEntity teacher, ClassEntity schoolClass) {
        if (isHomeroomTeacher(teacher, schoolClass)) {
            return;
        }
        boolean allowed = teachingEntries(teacher).stream()
                .anyMatch(entry -> Objects.equals(entry.getClassId(), schoolClass.getId()));
        if (!allowed) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Forbidden");
        }
    }

    private GradeAccessScope gradeAccessScope(
            TeacherProfileEntity teacher,
            ClassEntity schoolClass,
            List<SemesterEntity> semesters) {
        if (isHomeroomTeacher(teacher, schoolClass)) {
            return GradeAccessScope.all();
        }
        Set<SemesterSubjectKey> allowedPairs = semesters.stream()
                .flatMap(semester -> teacherEntriesForClassSemester(teacher, schoolClass.getId(), semester.getId()).stream())
                .map(entry -> new SemesterSubjectKey(entry.getSemesterId(), entry.getSubjectId()))
                .collect(Collectors.toSet());
        if (allowedPairs.isEmpty()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        return GradeAccessScope.restricted(allowedPairs);
    }

    private List<TimetableEntryEntity> teacherEntriesForClassSemester(
            TeacherProfileEntity teacher,
            UUID classId,
            UUID semesterId) {
        return timetableRepository.findByClassIdAndSemesterIdOrderByDayOfWeekAscPeriodNoAsc(classId, semesterId)
                .stream()
                .filter(entry -> isTeacherEntry(entry, teacher))
                .toList();
    }

    private List<TimetableEntryEntity> teachingEntries(TeacherProfileEntity teacher) {
        Map<UUID, TimetableEntryEntity> entriesById = timetableRepository.findByTeacherId(teacher.getUserId())
                .stream()
                .collect(Collectors.toMap(TimetableEntryEntity::getId, Function.identity()));
        if (hasText(teacher.getFullName())) {
            timetableRepository.findByTeacherNameIgnoreCase(teacher.getFullName())
                    .forEach(entry -> entriesById.putIfAbsent(entry.getId(), entry));
        }
        return List.copyOf(entriesById.values());
    }

    private String classSubjectLabel(TeacherProfileEntity teacher, ClassEntity schoolClass) {
        if (isHomeroomTeacher(teacher, schoolClass)) {
            return "T\u1EA5t c\u1EA3 m\u00F4n";
        }
        List<TimetableEntryEntity> entries = currentSemesterForClass(schoolClass)
                .map(semester -> teacherEntriesForClassSemester(teacher, schoolClass.getId(), semester.getId()))
                .filter(items -> !items.isEmpty())
                .orElseGet(() -> teachingEntries(teacher).stream()
                        .filter(entry -> Objects.equals(entry.getClassId(), schoolClass.getId()))
                        .toList());
        Map<UUID, SubjectEntity> subjects = subjectMap(entries.stream()
                .map(TimetableEntryEntity::getSubjectId)
                .collect(Collectors.toSet()));
        String subjectLabel = entries.stream()
                .map(TimetableEntryEntity::getSubjectId)
                .distinct()
                .map(subjects::get)
                .filter(Objects::nonNull)
                .map(SubjectEntity::getName)
                .distinct()
                .collect(Collectors.joining(", "));
        return subjectLabel.isBlank() ? "Theo ph\u00E2n c\u00F4ng" : subjectLabel;
    }

    private List<SemesterEntity> semestersForClassSchoolYear(ClassEntity schoolClass) {
        return semesterRepository.findBySchoolYearId(schoolClass.getSchoolYearId()).stream()
                .sorted(Comparator.comparing(SemesterEntity::getSemesterNo)
                        .thenComparing(SemesterEntity::getStartDate))
                .toList();
    }

    private PeriodResolution resolvePeriod(String periodId, ClassEntity schoolClass) {
        if (periodId == null || periodId.isBlank()) {
            SemesterEntity semester = currentSemesterForClass(schoolClass)
                    .or(() -> semestersForClassSchoolYear(schoolClass).stream().findFirst())
                    .orElseThrow(() -> notFound("Current semester was not found"));
            return toSemesterPeriodResolution(semester, requireSchoolYear(semester.getSchoolYearId()));
        }
        Optional<UUID> semesterUuid = parseUuid(periodId);
        if (semesterUuid.isPresent()) {
            SemesterEntity semester = semesterRepository.findById(semesterUuid.get())
                    .orElseThrow(() -> notFound("Academic period was not found"));
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
            return new PeriodResolution(toAcademicYearPeriod(schoolYear), semesters);
        }
        throw badRequest("Invalid period id");
    }

    private Optional<SemesterEntity> currentSemesterForClass(ClassEntity schoolClass) {
        return semesterRepository.findBySchoolYearId(schoolClass.getSchoolYearId()).stream()
                .filter(semester -> Boolean.TRUE.equals(semester.getCurrent()))
                .findFirst()
                .or(() -> schoolYearRepository.findByCurrentTrue()
                        .flatMap(currentSchoolYear -> semesterRepository
                                .findBySchoolYearIdAndCurrentTrue(currentSchoolYear.getId())));
    }

    private PeriodResolution toSemesterPeriodResolution(SemesterEntity semester, SchoolYearEntity schoolYear) {
        return new PeriodResolution(toPeriod(semester, schoolYear), List.of(semester));
    }

    private TeacherAcademicPeriodsResponse.Period toPeriod(SemesterEntity semester, SchoolYearEntity schoolYear) {
        return new TeacherAcademicPeriodsResponse.Period(
                "semester_" + semester.getSemesterNo() + "_" + schoolYearCode(schoolYear),
                semesterLabel(semester.getSemesterNo()),
                semesterTitle(semester.getSemesterNo()),
                formatSchoolYearLabel(schoolYear));
    }

    private TeacherAcademicPeriodsResponse.Period toAcademicYearPeriod(SchoolYearEntity schoolYear) {
        return new TeacherAcademicPeriodsResponse.Period(
                "year_" + schoolYearCode(schoolYear),
                "C\u1EA3 n\u0103m",
                "T\u1ED5ng k\u1EBFt c\u1EA3 n\u0103m",
                formatSchoolYearLabel(schoolYear));
    }

    private SubjectEntity resolveSubject(String subjectId) {
        if (!hasText(subjectId)) {
            throw badRequest("Subject id is required");
        }
        Optional<UUID> subjectUuid = parseUuid(subjectId);
        if (subjectUuid.isPresent()) {
            return subjectRepository.findById(subjectUuid.get())
                    .orElseThrow(() -> notFound("Subject was not found"));
        }
        return subjectRepository.findByCode(subjectId.toUpperCase(Locale.ROOT))
                .orElseThrow(() -> notFound("Subject was not found"));
    }

    private Map<UUID, String> teacherNameMap(
            TeacherProfileEntity currentTeacher,
            ClassEntity schoolClass,
            List<SemesterEntity> semesters) {
        Map<UUID, String> teacherNames = new LinkedHashMap<>();
        for (SemesterEntity semester : semesters) {
            timetableRepository.findByClassIdAndSemesterIdOrderByDayOfWeekAscPeriodNoAsc(
                            schoolClass.getId(), semester.getId())
                    .stream()
                    .filter(entry -> entry.getSubjectId() != null)
                    .filter(entry -> hasText(entry.getTeacherName()))
                    .forEach(entry -> teacherNames.putIfAbsent(entry.getSubjectId(), entry.getTeacherName()));
        }
        teachingEntries(currentTeacher).stream()
                .filter(entry -> Objects.equals(entry.getClassId(), schoolClass.getId()))
                .filter(entry -> hasText(entry.getTeacherName()))
                .forEach(entry -> teacherNames.putIfAbsent(entry.getSubjectId(), entry.getTeacherName()));
        return teacherNames;
    }

    private TeacherStudentGradeSummaryResponse.SubjectGrade toSubjectGrade(
            UUID subjectId,
            List<GradeEntity> grades,
            SubjectEntity subject,
            Map<UUID, String> teacherNames) {
        BigDecimal average = score(weightedAverageOnTen(grades));
        return new TeacherStudentGradeSummaryResponse.SubjectGrade(
                subjectClientId(subjectId, subject),
                subject == null ? null : subject.getName(),
                teacherNames.get(subjectId),
                average,
                toComponentScores(grades));
    }

    private List<TeacherStudentGradeSummaryResponse.ComponentScore> toComponentScores(List<GradeEntity> grades) {
        return grades.stream()
                .map(grade -> new TeacherStudentGradeSummaryResponse.ComponentScore(
                        grade.getTitle(),
                        normalizedScoreOnTen(grade)))
                .toList();
    }

    private BigDecimal weightedAverageOnTen(List<GradeEntity> grades) {
        BigDecimal weightedTotal = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (GradeEntity grade : grades) {
            if (grade.getScore() == null || grade.getMaxScore() == null
                    || grade.getMaxScore().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal weight = grade.getWeight() == null || grade.getWeight().compareTo(BigDecimal.ZERO) <= 0
                    ? BigDecimal.ONE
                    : grade.getWeight();
            BigDecimal normalizedScore = grade.getScore()
                    .multiply(TEN)
                    .divide(grade.getMaxScore(), 4, RoundingMode.HALF_UP);
            weightedTotal = weightedTotal.add(normalizedScore.multiply(weight));
            totalWeight = totalWeight.add(weight);
        }
        if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return weightedTotal.divide(totalWeight, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizedScoreOnTen(GradeEntity grade) {
        if (grade.getScore() == null || grade.getMaxScore() == null
                || grade.getMaxScore().compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return score(grade.getScore()
                .multiply(TEN)
                .divide(grade.getMaxScore(), 4, RoundingMode.HALF_UP));
    }

    private BigDecimal averageSubjectScores(List<TeacherStudentGradeSummaryResponse.SubjectGrade> subjects) {
        List<BigDecimal> averages = subjects.stream()
                .map(TeacherStudentGradeSummaryResponse.SubjectGrade::average)
                .filter(Objects::nonNull)
                .toList();
        if (averages.isEmpty()) {
            return null;
        }
        return score(averages.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(averages.size()), 4, RoundingMode.HALF_UP));
    }

    private BigDecimal score(BigDecimal value) {
        return value == null ? null : value.setScale(1, RoundingMode.HALF_UP);
    }

    private Map<UUID, SubjectEntity> subjectMap(Collection<UUID> subjectIds) {
        if (subjectIds == null || subjectIds.isEmpty()) {
            return Map.of();
        }
        return subjectRepository.findAllById(subjectIds).stream()
                .collect(Collectors.toMap(SubjectEntity::getId, Function.identity()));
    }

    private String subjectClientId(SubjectEntity subject) {
        return subjectClientId(subject.getId(), subject);
    }

    private String subjectClientId(UUID subjectId, SubjectEntity subject) {
        if (subject != null && hasText(subject.getCode())) {
            return subject.getCode().toLowerCase(Locale.ROOT);
        }
        return subjectId.toString();
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

    private TeacherNotificationsResponse.NotificationItem toNotificationItem(NotificationEntity notification) {
        String notificationType = normalizeNotificationType(notification.getNotificationType());
        String deepLink = normalizeDeepLink(notification.getDeepLink());
        return new TeacherNotificationsResponse.NotificationItem(
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

    private String normalizeDeepLink(String deepLink) {
        return deepLink == null || deepLink.isBlank() ? "" : deepLink.trim();
    }

    private String notificationCategory(String notificationType) {
        return switch (notificationType) {
            case "ACADEMIC", "GRADE", "SCORE", "EXAM", "TIMETABLE", "SCHEDULE" -> "academic";
            case "TUITION", "FEE", "PAYMENT" -> "tuition";
            case "REQUEST", "FORM", "APPLICATION" -> "request";
            case "SYSTEM", "ACCOUNT", "SECURITY" -> "system";
            case "EVENT", "NEWS", "ANNOUNCEMENT" -> "event";
            default -> "general";
        };
    }

    private String notificationActionLabel(String notificationType, String deepLink) {
        if (!hasText(deepLink)) {
            return "";
        }
        return switch (notificationType) {
            case "GRADE", "SCORE" -> "Xem \u0111i\u1EC3m";
            case "EXAM" -> "Xem l\u1ECBch thi";
            case "TIMETABLE", "SCHEDULE" -> "Xem th\u1EDDi kh\u00F3a bi\u1EC3u";
            case "TUITION", "FEE", "PAYMENT" -> "Xem h\u1ECDc ph\u00ED";
            case "REQUEST", "FORM", "APPLICATION" -> "Xem \u0111\u01A1n";
            default -> "Xem chi ti\u1EBFt";
        };
    }

    private boolean isHomeroomTeacher(TeacherProfileEntity teacher, ClassEntity schoolClass) {
        if (Objects.equals(schoolClass.getHomeroomTeacherId(), teacher.getUserId())) {
            return true;
        }
        return hasText(schoolClass.getHomeroomTeacherName())
                && hasText(teacher.getFullName())
                && schoolClass.getHomeroomTeacherName().trim().equalsIgnoreCase(teacher.getFullName());
    }

    private boolean isTeacherEntry(TimetableEntryEntity entry, TeacherProfileEntity teacher) {
        if (Objects.equals(entry.getTeacherId(), teacher.getUserId())) {
            return true;
        }
        return entry.getTeacherId() == null
                && hasText(entry.getTeacherName())
                && hasText(teacher.getFullName())
                && entry.getTeacherName().trim().equalsIgnoreCase(teacher.getFullName());
    }

    private String statusLabel(UserStatus status) {
        if (status == UserStatus.ACTIVE) {
            return "\u0110ang h\u1ECDc";
        }
        if (status == UserStatus.LOCKED) {
            return "T\u1EA1m kh\u00F3a";
        }
        if (status == UserStatus.INACTIVE) {
            return "Ngh\u1EC9 h\u1ECDc";
        }
        return "";
    }

    private String email(UserEntity user) {
        String username = user.getUsername();
        if (hasText(username) && username.contains("@")) {
            return username.trim();
        }
        return null;
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

    private String roleLabel(UserEntity user, TeacherProfileEntity teacher) {
        if (isHomeroomTeacher(user, teacher)) {
            return "Gi\u00E1o vi\u00EAn ch\u1EE7 nhi\u1EC7m";
        }
        return "Gi\u00E1o vi\u00EAn b\u1ED9 m\u00F4n";
    }

    private boolean isHomeroomTeacher(UserEntity user, TeacherProfileEntity teacher) {
        if (user.getRole() != null && "HOMEROOM_TEACHER".equals(user.getRole().getCode())) {
            return true;
        }
        if (!classRepository.findByHomeroomTeacherId(user.getId()).isEmpty()) {
            return true;
        }
        return teacher != null
                && hasText(teacher.getFullName())
                && !classRepository.findByHomeroomTeacherNameIgnoreCase(teacher.getFullName()).isEmpty();
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

    private Optional<UUID> parseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private TeacherProfileEntity requireTeacherProfile(UUID teacherUserId) {
        return teacherProfileRepository.findByUserId(teacherUserId)
                .orElseThrow(() -> notFound("Teacher profile not found"));
    }

    private UserEntity requireUser(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> notFound("User was not found"));
    }

    private ClassEntity requireClass(UUID classId) {
        if (classId == null) {
            throw badRequest("classId must not be null");
        }
        return classRepository.findById(classId).orElseThrow(() -> notFound("Class was not found"));
    }

    private ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    private ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record StudentAccessContext(
            TeacherProfileEntity teacher,
            UserEntity student,
            ClassEntity schoolClass,
            SchoolYearEntity schoolYear) {
    }

    private record PeriodResolution(
            TeacherAcademicPeriodsResponse.Period response,
            List<SemesterEntity> semesters) {
    }

    private record SemesterSubjectKey(UUID semesterId, UUID subjectId) {
    }

    private record GradeAccessScope(boolean allSubjects, Set<SemesterSubjectKey> allowedPairs) {

        static GradeAccessScope all() {
            return new GradeAccessScope(true, Set.of());
        }

        static GradeAccessScope restricted(Set<SemesterSubjectKey> allowedPairs) {
            return new GradeAccessScope(false, allowedPairs);
        }

        boolean canView(GradeEntity grade) {
            return allSubjects || allowedPairs.contains(new SemesterSubjectKey(
                    grade.getSemesterId(),
                    grade.getSubjectId()));
        }

        boolean canViewSubject(UUID subjectId) {
            return allSubjects || allowedPairs.stream()
                    .anyMatch(pair -> Objects.equals(pair.subjectId(), subjectId));
        }
    }
}
