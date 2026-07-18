package com.fschool.edu.fschool_backend.application.service;

import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.ClassEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.TeacherProfileEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.TimetableEntryEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.ClassJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.TeacherProfileJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.TimetableEntryJpaRepository;
import com.fschool.edu.fschool_backend.presentation.exception.ApiException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeacherGradeAccessService {

    private final TeacherProfileJpaRepository teacherProfileRepository;
    private final ClassJpaRepository classRepository;
    private final TimetableEntryJpaRepository timetableEntryRepository;

    public TeacherGradeAccessService(
            TeacherProfileJpaRepository teacherProfileRepository,
            ClassJpaRepository classRepository,
            TimetableEntryJpaRepository timetableEntryRepository) {
        this.teacherProfileRepository = teacherProfileRepository;
        this.classRepository = classRepository;
        this.timetableEntryRepository = timetableEntryRepository;
    }

    @Transactional(readOnly = true)
    public List<ClassEntity> accessibleClasses(UUID teacherUserId, Integer gradeLevel) {
        TeacherProfileEntity teacher = requireTeacherProfile(teacherUserId);
        Map<UUID, ClassEntity> classesById = new LinkedHashMap<>();
        homeroomClasses(teacher).forEach(schoolClass -> classesById.put(schoolClass.getId(), schoolClass));

        List<UUID> teachingClassIds = teachingEntries(teacher).stream()
                .map(TimetableEntryEntity::getClassId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        classRepository.findAllById(teachingClassIds)
                .forEach(schoolClass -> classesById.putIfAbsent(schoolClass.getId(), schoolClass));

        return classesById.values().stream()
                .filter(schoolClass -> gradeLevel == null
                        || Objects.equals(schoolClass.getGradeNumber(), gradeLevel.shortValue()))
                .sorted(java.util.Comparator.comparing(ClassEntity::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClassEntity> teachingClasses(UUID teacherUserId, Integer gradeLevel) {
        TeacherProfileEntity teacher = requireTeacherProfile(teacherUserId);
        return teachingClasses(teacher, teachingEntries(teacher), gradeLevel);
    }

    @Transactional(readOnly = true)
    public List<ClassEntity> teachingClasses(UUID teacherUserId, UUID semesterId, Integer gradeLevel) {
        TeacherProfileEntity teacher = requireTeacherProfile(teacherUserId);
        List<TimetableEntryEntity> entries = semesterId == null
                ? teachingEntries(teacher)
                : teachingEntriesForSemester(teacher, semesterId);
        return teachingClasses(teacher, entries, gradeLevel);
    }

    @Transactional(readOnly = true)
    public List<TimetableEntryEntity> teachingEntriesForSemester(UUID teacherUserId, UUID semesterId) {
        TeacherProfileEntity teacher = requireTeacherProfile(teacherUserId);
        return teachingEntriesForSemester(teacher, semesterId);
    }

    private List<ClassEntity> teachingClasses(
            TeacherProfileEntity teacher,
            List<TimetableEntryEntity> entries,
            Integer gradeLevel) {
        List<UUID> teachingClassIds = entries.stream()
                .map(TimetableEntryEntity::getClassId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return classRepository.findAllById(teachingClassIds).stream()
                .filter(schoolClass -> gradeLevel == null
                        || Objects.equals(schoolClass.getGradeNumber(), gradeLevel.shortValue()))
                .sorted(java.util.Comparator.comparing(ClassEntity::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TimetableEntryEntity> accessibleEntriesForClassSemester(
            UUID teacherUserId,
            UUID classId,
            UUID semesterId) {
        TeacherProfileEntity teacher = requireTeacherProfile(teacherUserId);
        List<TimetableEntryEntity> classEntries = timetableEntryRepository
                .findByClassIdAndSemesterIdOrderByDayOfWeekAscPeriodNoAsc(classId, semesterId);
        if (isHomeroomTeacher(teacher, classId)) {
            return classEntries;
        }
        return classEntries.stream()
                .filter(entry -> isTeacherEntry(entry, teacher))
                .toList();
    }

    @Transactional(readOnly = true)
    public void requireCanViewGrades(
            UUID teacherUserId,
            UUID classId,
            UUID semesterId,
            UUID subjectId) {
        TeacherProfileEntity teacher = requireTeacherProfile(teacherUserId);
        if (isHomeroomTeacher(teacher, classId)) {
            return;
        }
        boolean allowed = timetableEntryRepository
                .findByClassIdAndSemesterIdOrderByDayOfWeekAscPeriodNoAsc(classId, semesterId)
                .stream()
                .filter(entry -> isTeacherEntry(entry, teacher))
                .anyMatch(entry -> Objects.equals(entry.getSubjectId(), subjectId));
        if (!allowed) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Forbidden");
        }
    }

    @Transactional(readOnly = true)
    public void requireCanViewTimetable(
            UUID teacherUserId,
            UUID classId,
            UUID semesterId) {
        TeacherProfileEntity teacher = requireTeacherProfile(teacherUserId);
        boolean allowed = teachingEntriesForSemester(teacher, semesterId).stream()
                .anyMatch(entry -> Objects.equals(entry.getClassId(), classId));
        if (!allowed) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Forbidden");
        }
    }

    public List<ClassEntity> homeroomClasses(TeacherProfileEntity teacher) {
        Map<UUID, ClassEntity> classesById = new LinkedHashMap<>();
        classRepository.findByHomeroomTeacherId(teacher.getUserId())
                .forEach(schoolClass -> classesById.put(schoolClass.getId(), schoolClass));
        if (hasText(teacher.getFullName())) {
            classRepository.findByHomeroomTeacherNameIgnoreCase(teacher.getFullName())
                    .forEach(schoolClass -> classesById.putIfAbsent(schoolClass.getId(), schoolClass));
        }
        return classesById.values().stream()
                .sorted(java.util.Comparator.comparing(ClassEntity::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private TeacherProfileEntity requireTeacherProfile(UUID teacherUserId) {
        return teacherProfileRepository.findByUserId(teacherUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Teacher profile not found"));
    }

    private List<TimetableEntryEntity> teachingEntries(TeacherProfileEntity teacher) {
        Map<UUID, TimetableEntryEntity> entriesById = timetableEntryRepository.findByTeacherId(teacher.getUserId())
                .stream()
                .collect(Collectors.toMap(TimetableEntryEntity::getId, Function.identity()));
        if (hasText(teacher.getFullName())) {
            timetableEntryRepository.findByTeacherNameIgnoreCase(teacher.getFullName())
                    .forEach(entry -> entriesById.putIfAbsent(entry.getId(), entry));
        }
        return List.copyOf(entriesById.values());
    }

    private List<TimetableEntryEntity> teachingEntriesForSemester(TeacherProfileEntity teacher, UUID semesterId) {
        Map<UUID, TimetableEntryEntity> entriesById = timetableEntryRepository
                .findByTeacherIdAndSemesterIdOrderByDayOfWeekAscPeriodNoAsc(teacher.getUserId(), semesterId)
                .stream()
                .collect(Collectors.toMap(TimetableEntryEntity::getId, Function.identity()));
        if (hasText(teacher.getFullName())) {
            timetableEntryRepository.findByTeacherNameIgnoreCaseAndSemesterIdOrderByDayOfWeekAscPeriodNoAsc(
                            teacher.getFullName(), semesterId)
                    .forEach(entry -> entriesById.putIfAbsent(entry.getId(), entry));
        }
        return entriesById.values().stream()
                .sorted(java.util.Comparator
                        .comparing(TimetableEntryEntity::getDayOfWeek)
                        .thenComparing(TimetableEntryEntity::getStartTime)
                        .thenComparing(TimetableEntryEntity::getPeriodNo))
                .toList();
    }

    private boolean isHomeroomTeacher(TeacherProfileEntity teacher, UUID classId) {
        return homeroomClasses(teacher).stream()
                .anyMatch(schoolClass -> Objects.equals(schoolClass.getId(), classId));
    }

    private boolean isTeacherEntry(TimetableEntryEntity entry, TeacherProfileEntity teacher) {
        if (Objects.equals(entry.getTeacherId(), teacher.getUserId())) {
            return true;
        }
        return entry.getTeacherId() == null
                && hasText(entry.getTeacherName())
                && entry.getTeacherName().trim().equalsIgnoreCase(teacher.getFullName());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
