package com.fschool.edu.fschool_backend.infrastructure.persistence.repository;

import com.fschool.edu.fschool_backend.domain.enums.ContentStatus;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.NewsPostEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsPostJpaRepository extends JpaRepository<NewsPostEntity, UUID> {
    List<NewsPostEntity> findByStatusOrderByPublishedAtDesc(ContentStatus status);
    Page<NewsPostEntity> findByStatusOrderByPublishedAtDesc(ContentStatus status, Pageable pageable);
}
