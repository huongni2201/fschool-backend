package com.fschool.edu.fschool_backend.application.service;

import com.fschool.edu.fschool_backend.domain.enums.GradeType;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.GradeEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.SubjectEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.TimetableEntryEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.UserEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.ClassJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.GradeJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.SemesterJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.SubjectJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.TimetableEntryJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.UserJpaRepository;
import com.fschool.edu.fschool_backend.presentation.dto.response.AdminGradeResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.SubjectFilterResponse;
import com.fschool.edu.fschool_backend.presentation.exception.ApiException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminGradeService {

    private static final BigDecimal TEN = BigDecimal.TEN;

    private final GradeJpaRepository gradeRepository;
    private final UserJpaRepository userRepository;
    private final SubjectJpaRepository subjectRepository;
    private final ClassJpaRepository classRepository;
    private final SemesterJpaRepository semesterRepository;
    private final TimetableEntryJpaRepository timetableRepository;
    private final TeacherGradeAccessService teacherGradeAccessService;

    public AdminGradeService(
            GradeJpaRepository gradeRepository,
            UserJpaRepository userRepository,
            SubjectJpaRepository subjectRepository,
            ClassJpaRepository classRepository,
            SemesterJpaRepository semesterRepository,
            TimetableEntryJpaRepository timetableRepository,
            TeacherGradeAccessService teacherGradeAccessService) {
        this.gradeRepository = gradeRepository;
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
        this.classRepository = classRepository;
        this.semesterRepository = semesterRepository;
        this.timetableRepository = timetableRepository;
        this.teacherGradeAccessService = teacherGradeAccessService;
    }

    @Transactional(readOnly = true)
    public List<SubjectFilterResponse> getSubjects(UUID classId, UUID semesterId) {
        return getSubjects(classId, semesterId, null, true);
    }

    @Transactional(readOnly = true)
    public List<SubjectFilterResponse> getSubjects(
            UUID classId,
            UUID semesterId,
            UUID currentUserId,
            boolean admin) {
        if (classId == null || semesterId == null) {
            if (!admin) {
                return List.of();
            }
            return subjectRepository.findAll(Sort.by(Sort.Order.asc("name"))).stream()
                    .map(this::toSubjectFilterResponse)
                    .toList();
        }

        List<TimetableEntryEntity> entries = admin
                ? timetableRepository.findByClassIdAndSemesterIdOrderByDayOfWeekAscPeriodNoAsc(classId, semesterId)
                : teacherGradeAccessService.accessibleEntriesForClassSemester(currentUserId, classId, semesterId);
        List<UUID> subjectIds = entries.stream()
                .map(TimetableEntryEntity::getSubjectId)
                .distinct()
                .toList();
        if (subjectIds.isEmpty()) {
            return List.of();
        }

        Map<UUID, SubjectEntity> subjectsById = subjectRepository.findAllById(subjectIds).stream()
                .collect(Collectors.toMap(SubjectEntity::getId, Function.identity()));
        return subjectIds.stream()
                .map(subjectsById::get)
                .filter(subject -> subject != null)
                .sorted(Comparator.comparing(SubjectEntity::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toSubjectFilterResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminGradeResponse> getGrades(UUID classId, UUID semesterId, UUID subjectId, String search) {
        return getGrades(classId, semesterId, subjectId, search, null, true);
    }

    @Transactional(readOnly = true)
    public List<AdminGradeResponse> getGrades(
            UUID classId,
            UUID semesterId,
            UUID subjectId,
            String search,
            UUID currentUserId,
            boolean admin) {
        validateGradeFilters(classId, semesterId, subjectId);
        requireFiltersExist(classId, semesterId, subjectId);
        if (!admin) {
            teacherGradeAccessService.requireCanViewGrades(currentUserId, classId, semesterId, subjectId);
        }

        List<UserEntity> students = userRepository.findStudentsByClassIdAndSearch(classId, normalizeSearch(search));
        if (students.isEmpty()) {
            return List.of();
        }

        List<UUID> studentIds = students.stream().map(UserEntity::getId).toList();
        Map<UUID, List<GradeEntity>> gradesByStudent = gradeRepository
                .findByUserIdInAndSemesterIdAndSubjectId(studentIds, semesterId, subjectId)
                .stream()
                .collect(Collectors.groupingBy(GradeEntity::getUserId));

        return students.stream()
                .map(student -> toGradeResponse(student, gradesByStudent.getOrDefault(student.getId(), List.of())))
                .toList();
    }

    private SubjectFilterResponse toSubjectFilterResponse(SubjectEntity subject) {
        return new SubjectFilterResponse(subject.getId(), subject.getCode(), subject.getName());
    }

    private AdminGradeResponse toGradeResponse(UserEntity student, List<GradeEntity> grades) {
        BigDecimal averageScore = score(weightedAverage(grades));
        return new AdminGradeResponse(
                student.getId(),
                student.getStudentCode(),
                student.getFullName(),
                score(typeAverage(grades, GradeType.REGULAR)),
                score(typeAverage(grades, GradeType.MIDTERM)),
                score(typeAverage(grades, GradeType.FINAL)),
                averageScore,
                classification(averageScore));
    }

    private BigDecimal typeAverage(List<GradeEntity> grades, GradeType gradeType) {
        return weightedAverage(grades.stream()
                .filter(grade -> grade.getGradeType() == gradeType)
                .toList());
    }

    private BigDecimal weightedAverage(Collection<GradeEntity> grades) {
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (GradeEntity grade : grades) {
            if (grade.getScore() == null || grade.getWeight() == null || grade.getMaxScore() == null
                    || grade.getMaxScore().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal normalizedScore = grade.getScore()
                    .multiply(TEN)
                    .divide(grade.getMaxScore(), 4, RoundingMode.HALF_UP);
            weightedSum = weightedSum.add(normalizedScore.multiply(grade.getWeight()));
            totalWeight = totalWeight.add(grade.getWeight());
        }
        if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return weightedSum.divide(totalWeight, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal score(BigDecimal value) {
        return value == null ? null : value.setScale(1, RoundingMode.HALF_UP);
    }

    private String classification(BigDecimal averageScore) {
        if (averageScore == null) {
            return null;
        }
        if (averageScore.compareTo(new BigDecimal("8.0")) >= 0) {
            return "Giỏi";
        }
        if (averageScore.compareTo(new BigDecimal("6.5")) >= 0) {
            return "Khá";
        }
        if (averageScore.compareTo(new BigDecimal("5.0")) >= 0) {
            return "Trung bình";
        }
        return "Yếu";
    }

    private void validateGradeFilters(UUID classId, UUID semesterId, UUID subjectId) {
        if (classId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "classId must not be null");
        }
        if (semesterId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "semesterId must not be null");
        }
        if (subjectId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "subjectId must not be null");
        }
    }

    private void requireFiltersExist(UUID classId, UUID semesterId, UUID subjectId) {
        if (!classRepository.existsById(classId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Class was not found");
        }
        if (!semesterRepository.existsById(semesterId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Semester was not found");
        }
        if (!subjectRepository.existsById(subjectId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Subject was not found");
        }
    }

    private String normalizeSearch(String search) {
        return search == null ? null : search.trim();
    }
}
