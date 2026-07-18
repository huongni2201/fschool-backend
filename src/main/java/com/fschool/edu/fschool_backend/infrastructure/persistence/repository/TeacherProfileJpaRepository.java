package com.fschool.edu.fschool_backend.infrastructure.persistence.repository;

import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.TeacherProfileEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherProfileJpaRepository extends JpaRepository<TeacherProfileEntity, UUID> {
    Optional<TeacherProfileEntity> findByUserId(UUID userId);

    List<TeacherProfileEntity> findByUserIdIn(Collection<UUID> userIds);

    List<TeacherProfileEntity> findByFullNameIn(Collection<String> fullNames);
}
