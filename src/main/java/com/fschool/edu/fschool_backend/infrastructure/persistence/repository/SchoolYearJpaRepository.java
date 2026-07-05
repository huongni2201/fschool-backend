package com.fschool.edu.fschool_backend.infrastructure.persistence.repository;

import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.SchoolYearEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolYearJpaRepository extends JpaRepository<SchoolYearEntity, UUID> {
    Optional<SchoolYearEntity> findByCurrentTrue();
}
