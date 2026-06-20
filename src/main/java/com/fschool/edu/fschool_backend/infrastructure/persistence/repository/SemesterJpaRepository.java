package com.fschool.edu.fschool_backend.infrastructure.persistence.repository;

import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.SemesterEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SemesterJpaRepository extends JpaRepository<SemesterEntity, UUID> {
    List<SemesterEntity> findBySchoolYearId(UUID schoolYearId);
    Optional<SemesterEntity> findBySchoolYearIdAndCurrentTrue(UUID schoolYearId);
}
