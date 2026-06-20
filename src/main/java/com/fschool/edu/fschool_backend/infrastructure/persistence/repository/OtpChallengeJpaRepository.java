package com.fschool.edu.fschool_backend.infrastructure.persistence.repository;

import com.fschool.edu.fschool_backend.domain.enums.OtpPurpose;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.OtpChallengeEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpChallengeJpaRepository extends JpaRepository<OtpChallengeEntity, UUID> {
    List<OtpChallengeEntity> findByPhoneAndPurposeOrderByExpiresAtDesc(String phone, OtpPurpose purpose);
}
