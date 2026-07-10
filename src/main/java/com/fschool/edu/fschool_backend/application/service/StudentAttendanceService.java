package com.fschool.edu.fschool_backend.application.service;

import com.fschool.edu.fschool_backend.domain.enums.AttendanceStatus;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.AttendanceRecordEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.SubjectEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.UserEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.AttendanceRecordJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.SubjectJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.UserJpaRepository;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentAttendanceResponse;
import com.fschool.edu.fschool_backend.presentation.exception.ApiException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentAttendanceService {

    private static final ZoneId ATTENDANCE_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final AttendanceRecordJpaRepository attendanceRepository;
    private final UserJpaRepository userRepository;
    private final SubjectJpaRepository subjectRepository;

    public StudentAttendanceService(
            AttendanceRecordJpaRepository attendanceRepository,
            UserJpaRepository userRepository,
            SubjectJpaRepository subjectRepository) {
        this.attendanceRepository = attendanceRepository;
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
    }

    @Transactional(readOnly = true)
    public StudentAttendanceResponse getStudentAttendance(UUID studentId, LocalDate from, LocalDate to) {
        requireUser(studentId);
        LocalDate resolvedTo = to == null ? LocalDate.now(ATTENDANCE_ZONE) : to;
        LocalDate resolvedFrom = from == null ? resolvedTo.withDayOfMonth(1) : from;
        if (resolvedTo.isBefore(resolvedFrom)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid attendance date range");
        }

        List<AttendanceRecordEntity> records = attendanceRepository
                .findByStudentIdAndAttendanceDateBetweenOrderByAttendanceDateAscPeriodNoAscStartTimeAsc(
                        studentId,
                        resolvedFrom,
                        resolvedTo);
        Map<UUID, SubjectEntity> subjects = subjectRepository.findAllById(records.stream()
                        .map(AttendanceRecordEntity::getSubjectId)
                        .filter(id -> id != null)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(SubjectEntity::getId, Function.identity()));

        return new StudentAttendanceResponse(
                toSummary(records),
                records.stream()
                        .map(record -> toItem(record, subjects.get(record.getSubjectId())))
                        .toList());
    }

    private UserEntity requireUser(UUID studentId) {
        return userRepository.findById(studentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Student was not found"));
    }

    private StudentAttendanceResponse.Summary toSummary(List<AttendanceRecordEntity> records) {
        return new StudentAttendanceResponse.Summary(
                records.size(),
                count(records, AttendanceStatus.PRESENT),
                count(records, AttendanceStatus.ABSENT),
                count(records, AttendanceStatus.LATE),
                count(records, AttendanceStatus.EXCUSED));
    }

    private int count(List<AttendanceRecordEntity> records, AttendanceStatus status) {
        return (int) records.stream()
                .filter(record -> record.getStatus() == status)
                .count();
    }

    private StudentAttendanceResponse.Item toItem(AttendanceRecordEntity record, SubjectEntity subject) {
        AttendanceStatus status = record.getStatus() == null ? AttendanceStatus.UNKNOWN : record.getStatus();
        return new StudentAttendanceResponse.Item(
                record.getId().toString(),
                record.getAttendanceDate(),
                subject == null ? null : subject.getName(),
                record.getPeriodNo() == null ? null : record.getPeriodNo().toString(),
                timeRange(record.getStartTime(), record.getEndTime()),
                record.getTeacherName(),
                statusCode(status),
                statusLabel(status),
                record.getNote() == null ? "" : record.getNote());
    }

    private String timeRange(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            return "";
        }
        return startTime.format(TIME_FORMATTER) + " - " + endTime.format(TIME_FORMATTER);
    }

    private String statusCode(AttendanceStatus status) {
        return status.name().toLowerCase(Locale.ROOT);
    }

    private String statusLabel(AttendanceStatus status) {
        return switch (status) {
            case PRESENT -> "C\u00f3 m\u1eb7t";
            case ABSENT -> "V\u1eafng m\u1eb7t";
            case LATE -> "\u0110i mu\u1ed9n";
            case EXCUSED -> "V\u1eafng c\u00f3 ph\u00e9p";
            case UNKNOWN -> "Ch\u01b0a c\u00f3 d\u1eef li\u1ec7u";
        };
    }
}
