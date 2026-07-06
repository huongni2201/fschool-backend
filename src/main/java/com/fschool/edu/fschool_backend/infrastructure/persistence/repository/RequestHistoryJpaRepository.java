package com.fschool.edu.fschool_backend.infrastructure.persistence.repository;

import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.RequestHistoryEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestHistoryJpaRepository extends JpaRepository<RequestHistoryEntity, UUID> {

    List<RequestHistoryEntity> findByRequestIdOrderByCreatedAtAsc(UUID requestId);
}
