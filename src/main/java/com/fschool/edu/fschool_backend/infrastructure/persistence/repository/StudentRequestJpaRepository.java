package com.fschool.edu.fschool_backend.infrastructure.persistence.repository;

import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.StudentRequestEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StudentRequestJpaRepository
        extends JpaRepository<StudentRequestEntity, UUID>, JpaSpecificationExecutor<StudentRequestEntity> {

    Optional<StudentRequestEntity> findByIdAndStudentId(UUID id, UUID studentId);

    Optional<StudentRequestEntity> findByRequestNumberAndStudentId(String requestNumber, UUID studentId);

    boolean existsByRequestNumber(String requestNumber);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant startInclusive, Instant endExclusive);
}
