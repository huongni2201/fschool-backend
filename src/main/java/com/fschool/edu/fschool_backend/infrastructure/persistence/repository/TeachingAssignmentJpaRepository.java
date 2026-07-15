package com.fschool.edu.fschool_backend.infrastructure.persistence.repository;

import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.TeachingAssignmentEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeachingAssignmentJpaRepository extends JpaRepository<TeachingAssignmentEntity, UUID> {
    List<TeachingAssignmentEntity> findByTeacherIdAndActiveTrue(UUID teacherId);
}
