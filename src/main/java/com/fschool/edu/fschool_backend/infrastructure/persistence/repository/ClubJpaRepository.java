package com.fschool.edu.fschool_backend.infrastructure.persistence.repository;

import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.ClubEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubJpaRepository extends JpaRepository<ClubEntity, UUID> {

    List<ClubEntity> findByActiveTrueOrderBySortOrderAscNameAsc();

    Optional<ClubEntity> findByPublicIdIgnoreCaseOrCodeIgnoreCase(String publicId, String code);
}
