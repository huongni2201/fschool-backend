package com.fschool.edu.fschool_backend.infrastructure.persistence.repository;

import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.RequestTypeEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestTypeJpaRepository extends JpaRepository<RequestTypeEntity, UUID> {

    List<RequestTypeEntity> findByActiveTrueOrderBySortOrderAsc();

    Optional<RequestTypeEntity> findByCode(String code);
}
