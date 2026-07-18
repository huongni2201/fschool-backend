package com.fschool.edu.fschool_backend.infrastructure.persistence.repository;

import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.TimetableEntryEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimetableEntryJpaRepository extends JpaRepository<TimetableEntryEntity, UUID> {
    List<TimetableEntryEntity> findByClassIdAndSemesterIdOrderByDayOfWeekAscPeriodNoAsc(UUID classId, UUID semesterId);
    List<TimetableEntryEntity> findByClassIdAndSemesterIdAndDayOfWeekOrderByPeriodNoAsc(
            UUID classId, UUID semesterId, Short dayOfWeek);
    Optional<TimetableEntryEntity> findByClassIdAndSemesterIdAndDayOfWeekAndPeriodNo(
            UUID classId, UUID semesterId, Short dayOfWeek, Short periodNo);
    List<TimetableEntryEntity> findByTeacherIdAndSemesterIdAndDayOfWeekOrderByStartTimeAscPeriodNoAsc(
            UUID teacherId, UUID semesterId, Short dayOfWeek);
    List<TimetableEntryEntity> findByTeacherIdAndSemesterIdOrderByDayOfWeekAscPeriodNoAsc(
            UUID teacherId, UUID semesterId);
    List<TimetableEntryEntity> findByTeacherId(UUID teacherId);

    List<TimetableEntryEntity> findByTeacherNameIgnoreCase(String teacherName);

    List<TimetableEntryEntity> findByTeacherNameIgnoreCaseAndSemesterIdAndDayOfWeekOrderByStartTimeAscPeriodNoAsc(
            String teacherName, UUID semesterId, Short dayOfWeek);
    List<TimetableEntryEntity> findByTeacherNameIgnoreCaseAndSemesterIdOrderByDayOfWeekAscPeriodNoAsc(
            String teacherName, UUID semesterId);
    List<TimetableEntryEntity> findByClassIdInAndSemesterIdAndSubjectIdInAndDayOfWeekOrderByStartTimeAscPeriodNoAsc(
            Collection<UUID> classIds,
            UUID semesterId,
            Collection<UUID> subjectIds,
            Short dayOfWeek);
}
