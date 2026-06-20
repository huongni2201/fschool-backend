package com.fschool.edu.fschool_backend.infrastructure.persistence.repository;

import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.AssignmentEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentJpaRepository extends JpaRepository<AssignmentEntity, UUID> {
    List<AssignmentEntity> findByClassId(UUID classId);
    List<AssignmentEntity> findByClassIdAndSemesterId(UUID classId, UUID semesterId);
    List<AssignmentEntity> findByClassIdAndSubjectIdAndSemesterId(UUID classId, UUID subjectId, UUID semesterId);
}
