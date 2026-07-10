package com.fschool.edu.fschool_backend.infrastructure.persistence.repository;

import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.AttendanceRecordEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRecordJpaRepository extends JpaRepository<AttendanceRecordEntity, UUID> {

    List<AttendanceRecordEntity> findByStudentIdAndAttendanceDateBetweenOrderByAttendanceDateAscPeriodNoAscStartTimeAsc(
            UUID studentId,
            LocalDate from,
            LocalDate to);
}
