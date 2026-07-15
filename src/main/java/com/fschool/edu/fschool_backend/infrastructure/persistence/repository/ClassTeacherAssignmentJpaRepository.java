package com.fschool.edu.fschool_backend.infrastructure.persistence.repository;

import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.ClassTeacherAssignmentEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassTeacherAssignmentJpaRepository extends JpaRepository<ClassTeacherAssignmentEntity, UUID> {
    List<ClassTeacherAssignmentEntity> findByTeacherIdAndRoleAndActiveTrue(UUID teacherId, String role);
}
