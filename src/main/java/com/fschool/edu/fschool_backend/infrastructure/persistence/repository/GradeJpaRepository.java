package com.fschool.edu.fschool_backend.infrastructure.persistence.repository;

import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.GradeEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GradeJpaRepository extends JpaRepository<GradeEntity, UUID> {
    List<GradeEntity> findByUserIdOrderByAssessmentDateDesc(UUID userId);
    List<GradeEntity> findByUserIdAndSemesterIdOrderByAssessmentDateDesc(UUID userId, UUID semesterId);
    List<GradeEntity> findByUserIdAndSemesterIdInOrderByAssessmentDateDesc(UUID userId, List<UUID> semesterIds);
    List<GradeEntity> findByUserIdAndSubjectIdAndSemesterId(UUID userId, UUID subjectId, UUID semesterId);
    List<GradeEntity> findByUserIdAndSubjectIdAndSemesterIdInOrderByAssessmentDateAsc(
            UUID userId, UUID subjectId, List<UUID> semesterIds);
}
