package com.fschool.edu.fschool_backend.infrastructure.persistence.repository;

import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.TimetableEntryEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimetableEntryJpaRepository extends JpaRepository<TimetableEntryEntity, UUID> {
    List<TimetableEntryEntity> findByClassIdAndSemesterIdOrderByDayOfWeekAscPeriodNoAsc(UUID classId, UUID semesterId);
    List<TimetableEntryEntity> findByClassIdAndSemesterIdAndDayOfWeekOrderByPeriodNoAsc(
            UUID classId, UUID semesterId, Short dayOfWeek);
}
