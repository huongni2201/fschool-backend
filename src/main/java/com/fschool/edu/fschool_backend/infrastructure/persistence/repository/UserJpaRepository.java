package com.fschool.edu.fschool_backend.infrastructure.persistence.repository;

import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.UserEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByPhone(String phone);
    Optional<UserEntity> findByStudentCode(String studentCode);
    boolean existsByPhone(String phone);
    boolean existsByStudentCode(String studentCode);
    long countByClassId(UUID classId);
    List<UserEntity> findByClassIdIn(Collection<UUID> classIds);
    List<UserEntity> findByGuardianPhone(String guardianPhone);
    List<UserEntity> findByGuardianPhoneIn(Collection<String> guardianPhones);
}
