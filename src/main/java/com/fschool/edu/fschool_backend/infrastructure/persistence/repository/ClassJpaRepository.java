package com.fschool.edu.fschool_backend.infrastructure.persistence.repository;

import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.ClassEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ClassJpaRepository extends JpaRepository<ClassEntity, UUID> {
    List<ClassEntity> findByHomeroomTeacherId(UUID homeroomTeacherId);

    List<ClassEntity> findByHomeroomTeacherNameIgnoreCase(String homeroomTeacherName);

    List<ClassEntity> findByNameIgnoreCase(String name);

    List<ClassEntity> findByGradeNumberOrderByNameAsc(Short gradeNumber);

    @Query("""
            select distinct c.gradeNumber
            from ClassEntity c
            order by c.gradeNumber
            """)
    List<Short> findDistinctGradeNumbers();
}
