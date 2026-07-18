package com.fschool.edu.fschool_backend.infrastructure.persistence.repository;

import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.ExamEntity;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamJpaRepository extends JpaRepository<ExamEntity, UUID> {
    List<ExamEntity> findByClassIdAndSemesterIdOrderByExamDateAscStartTimeAsc(UUID classId, UUID semesterId);
    List<ExamEntity> findByExamDateGreaterThanEqualOrderByExamDateAscStartTimeAsc(LocalDate examDate);
    List<ExamEntity> findByClassIdInAndSubjectIdInAndExamDateGreaterThanEqualOrderByExamDateAscStartTimeAsc(
            Collection<UUID> classIds,
            Collection<UUID> subjectIds,
            LocalDate examDate);
}
