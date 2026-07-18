package com.fschool.edu.fschool_backend.infrastructure.persistence.repository;

import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.UploadedFileEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadedFileJpaRepository extends JpaRepository<UploadedFileEntity, UUID> {

    Optional<UploadedFileEntity> findByFileCode(String fileCode);

    List<UploadedFileEntity> findByOrderByCreatedAtDesc(Pageable pageable);

    List<UploadedFileEntity> findByPurposeNotOrderByCreatedAtDesc(String purpose, Pageable pageable);
}
