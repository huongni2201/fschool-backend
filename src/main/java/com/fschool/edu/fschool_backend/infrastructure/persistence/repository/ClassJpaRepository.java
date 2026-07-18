package com.fschool.edu.fschool_backend.infrastructure.persistence.repository;

import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.ClassEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassJpaRepository extends JpaRepository<ClassEntity, UUID> {
    List<ClassEntity> findByHomeroomTeacherNameIgnoreCase(String homeroomTeacherName);
}
