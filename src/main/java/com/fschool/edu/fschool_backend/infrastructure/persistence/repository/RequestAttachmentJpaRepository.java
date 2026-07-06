package com.fschool.edu.fschool_backend.infrastructure.persistence.repository;

import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.RequestAttachmentEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestAttachmentJpaRepository extends JpaRepository<RequestAttachmentEntity, UUID> {

    List<RequestAttachmentEntity> findByRequestId(UUID requestId);
}
