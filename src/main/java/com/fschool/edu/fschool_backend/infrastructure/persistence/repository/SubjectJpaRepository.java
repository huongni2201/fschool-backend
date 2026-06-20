package com.fschool.edu.fschool_backend.infrastructure.persistence.repository;

import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.SubjectEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectJpaRepository extends JpaRepository<SubjectEntity, UUID> {
    Optional<SubjectEntity> findByCode(String code);
}
