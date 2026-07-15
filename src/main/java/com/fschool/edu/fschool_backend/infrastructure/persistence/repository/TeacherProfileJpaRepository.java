package com.fschool.edu.fschool_backend.infrastructure.persistence.repository;

import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.TeacherProfileEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherProfileJpaRepository extends JpaRepository<TeacherProfileEntity, UUID> {
    Optional<TeacherProfileEntity> findByUserId(UUID userId);
}
