package com.fschool.edu.fschool_backend.infrastructure.persistence.repository;

import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.UserEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByPhone(String phone);
    @Query("""
            select u
            from UserEntity u
            where u.phone = :login
               or lower(u.username) = lower(:login)
            """)
    Optional<UserEntity> findByPhoneOrUsername(@Param("login") String login);
    Optional<UserEntity> findByStudentCode(String studentCode);
    boolean existsByPhone(String phone);
    boolean existsByStudentCode(String studentCode);
    @Query("""
            select count(u)
            from UserEntity u
            where u.role.code = :roleCode
    """)
    long countByRoleCode(@Param("roleCode") String roleCode);
    long countByClassId(UUID classId);
    @Query("""
            select count(u)
            from UserEntity u
            where u.classId = :classId
              and u.role.code = 'STUDENT'
            """)
    long countStudentsByClassId(@Param("classId") UUID classId);
    List<UserEntity> findByClassIdIn(Collection<UUID> classIds);
    List<UserEntity> findByGuardianPhone(String guardianPhone);
    List<UserEntity> findByGuardianPhoneIn(Collection<String> guardianPhones);

    @Query("""
            select u
            from UserEntity u
            where u.classId = :classId
              and u.role.code = 'STUDENT'
              and (
                  :search is null
                  or :search = ''
                  or lower(u.fullName) like lower(concat('%', :search, '%'))
                  or lower(u.studentCode) like lower(concat('%', :search, '%'))
              )
            order by u.fullName asc, u.studentCode asc
            """)
    List<UserEntity> findStudentsByClassIdAndSearch(
            @Param("classId") UUID classId,
            @Param("search") String search);
}
