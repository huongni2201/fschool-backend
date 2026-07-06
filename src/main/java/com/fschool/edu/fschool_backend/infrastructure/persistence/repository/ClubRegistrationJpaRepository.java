package com.fschool.edu.fschool_backend.infrastructure.persistence.repository;

import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.ClubRegistrationEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubRegistrationJpaRepository extends JpaRepository<ClubRegistrationEntity, UUID> {

    List<ClubRegistrationEntity> findByStudentIdAndClubIdIn(UUID studentId, Collection<UUID> clubIds);

    Optional<ClubRegistrationEntity> findByStudentIdAndClubId(UUID studentId, UUID clubId);
}
