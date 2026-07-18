package com.fschool.edu.fschool_backend.infrastructure.persistence.repository;

import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.NotificationEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, UUID> {
    List<NotificationEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    long countByUserIdAndReadFalse(UUID userId);

    long countByReadFalse();

    List<NotificationEntity> findTop5ByOrderByCreatedAtDesc();

    List<NotificationEntity> findByOrderByCreatedAtDesc(Pageable pageable);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant startInclusive, Instant endExclusive);
}
