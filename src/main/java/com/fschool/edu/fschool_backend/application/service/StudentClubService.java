package com.fschool.edu.fschool_backend.application.service;

import com.fschool.edu.fschool_backend.domain.enums.ClubRegistrationStatus;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.ClubEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.entity.ClubRegistrationEntity;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.ClubJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.ClubRegistrationJpaRepository;
import com.fschool.edu.fschool_backend.infrastructure.persistence.repository.UserJpaRepository;
import com.fschool.edu.fschool_backend.presentation.dto.request.ClubRegistrationRequest;
import com.fschool.edu.fschool_backend.presentation.dto.response.ClubRegistrationCancellationResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.ClubRegistrationResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentClubDetailResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.StudentClubListResponse;
import com.fschool.edu.fschool_backend.presentation.exception.ApiException;

import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentClubService {

  private static final ZoneId CLUB_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
  private static final String STATUS_OPEN = "OPEN";
  private static final String STATUS_CLOSED = "CLOSED";

  private final ClubJpaRepository clubRepository;
  private final ClubRegistrationJpaRepository registrationRepository;
  private final UserJpaRepository userRepository;

  @Transactional(readOnly = true)
  public StudentClubListResponse getStudentClubs(UUID studentId) {
    requireStudent(studentId);
    var clubs = clubRepository.findByActiveTrueOrderBySortOrderAscNameAsc();
    Map<UUID, ClubRegistrationEntity> registrations = registrationsByClubId(studentId, clubs.stream()
        .map(ClubEntity::getId)
        .toList());

    return new StudentClubListResponse(clubs.stream()
        .map(club -> toListItem(club, registrations.get(club.getId())))
        .toList());
  }

  @Transactional(readOnly = true)
  public StudentClubDetailResponse getStudentClub(UUID studentId, String clubId) {
    requireStudent(studentId);
    ClubEntity club = resolveClub(clubId);
    ClubRegistrationEntity registration = registrationRepository
        .findByStudentIdAndClubId(studentId, club.getId())
        .orElse(null);
    return toDetailResponse(club, registration);
  }

  @Transactional
  public ClubRegistrationResponse register(UUID studentId, String clubId, ClubRegistrationRequest request) {
    requireStudent(studentId);
    ClubEntity club = resolveClub(clubId);
    if (!isOpenForRegistration(club)) {
      throw new ApiException(HttpStatus.CONFLICT, "Cau lac bo da dong dang ky");
    }

    Instant now = Instant.now();
    ClubRegistrationEntity registration = registrationRepository
        .findByStudentIdAndClubId(studentId, club.getId())
        .orElseGet(ClubRegistrationEntity::new);
    if (registration.getId() != null
        && registration.getStatus() == ClubRegistrationStatus.PENDING) {
      throw new ApiException(HttpStatus.CONFLICT, "Hoc sinh da dang ky cau lac bo nay");
    }
    if (registration.getId() != null
        && registration.getStatus() == ClubRegistrationStatus.JOINED) {
      throw new ApiException(HttpStatus.CONFLICT, "Hoc sinh dang tham gia cau lac bo nay");
    }

    registration.setStudentId(studentId);
    registration.setClubId(club.getId());
    registration.setStatus(ClubRegistrationStatus.PENDING);
    registration.setReason(normalizeReason(request));
    registration.setCancellationReason(null);
    registration.setRegisteredAt(now);
    registration.setApprovedAt(null);
    registration.setCancelledAt(null);

    ClubRegistrationEntity savedRegistration = registrationRepository.save(registration);
    return new ClubRegistrationResponse(
        club.getPublicId(),
        savedRegistration.getId().toString(),
        savedRegistration.getStatus().name(),
        statusLabel(savedRegistration.getStatus().name()),
        toOffsetDateTime(savedRegistration.getRegisteredAt()));
  }

  @Transactional
  public ClubRegistrationCancellationResponse cancelRegistration(
      UUID studentId,
      String clubId,
      ClubRegistrationRequest request) {
    requireStudent(studentId);
    ClubEntity club = resolveClub(clubId);
    Instant now = Instant.now();
    Optional<ClubRegistrationEntity> existingRegistration = registrationRepository
        .findByStudentIdAndClubId(studentId, club.getId());

    existingRegistration
        .filter(registration -> registration.getStatus() == ClubRegistrationStatus.PENDING
            || registration.getStatus() == ClubRegistrationStatus.JOINED)
        .ifPresent(registration -> {
          boolean wasJoined = registration.getStatus() == ClubRegistrationStatus.JOINED;
          registration.setStatus(ClubRegistrationStatus.LEFT);
          registration.setCancellationReason(normalizeReason(request));
          registration.setCancelledAt(now);
          registrationRepository.save(registration);
          if (wasJoined && club.getMemberCount() > 0) {
            club.setMemberCount(club.getMemberCount() - 1);
          }
        });

    String status = openOrClosedStatus(club);
    return new ClubRegistrationCancellationResponse(
        club.getPublicId(),
        status,
        statusLabel(status),
        toOffsetDateTime(now));
  }

  private Map<UUID, ClubRegistrationEntity> registrationsByClubId(UUID studentId, Collection<UUID> clubIds) {
    if (clubIds.isEmpty()) {
      return Map.of();
    }
    return registrationRepository.findByStudentIdAndClubIdIn(studentId, clubIds).stream()
        .collect(Collectors.toMap(
            ClubRegistrationEntity::getClubId,
            Function.identity(),
            (first, second) -> first));
  }

  private ClubEntity resolveClub(String clubId) {
    if (clubId == null || clubId.isBlank()) {
      throw new ApiException(HttpStatus.NOT_FOUND, "Club was not found");
    }
    String normalizedClubId = clubId.trim();
    Optional<ClubEntity> byUuid = parseUuid(normalizedClubId).flatMap(clubRepository::findById);
    return byUuid.or(() -> clubRepository.findByPublicIdIgnoreCaseOrCodeIgnoreCase(
            normalizedClubId,
            normalizedClubId))
        .filter(ClubEntity::isActive)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club was not found"));
  }

  private Optional<UUID> parseUuid(String value) {
    try {
      return Optional.of(UUID.fromString(value));
    } catch (IllegalArgumentException exception) {
      return Optional.empty();
    }
  }

  private void requireStudent(UUID studentId) {
    if (!userRepository.existsById(studentId)) {
      throw new ApiException(HttpStatus.NOT_FOUND, "Student was not found");
    }
  }

  private StudentClubListResponse.Item toListItem(ClubEntity club, ClubRegistrationEntity registration) {
    String status = status(club, registration);
    return new StudentClubListResponse.Item(
        club.getPublicId(),
        club.getCode(),
        club.getName(),
        club.getDescription(),
        club.getTeacherName(),
        club.getLocation(),
        scheduleLabel(club),
        club.getMemberCount(),
        status,
        statusLabel(status));
  }

  private StudentClubDetailResponse toDetailResponse(ClubEntity club, ClubRegistrationEntity registration) {
    String status = status(club, registration);
    return new StudentClubDetailResponse(
        club.getPublicId(),
        club.getCode(),
        club.getName(),
        club.getDescription(),
        new StudentClubDetailResponse.Teacher(
            club.getTeacherCode(),
            club.getTeacherName(),
            club.getTeacherPhone(),
            club.getTeacherEmail()),
        club.getLocation(),
        new StudentClubDetailResponse.Schedule(
            club.getWeekday(),
            formatTime(club.getStartTime()),
            formatTime(club.getEndTime())),
        club.getMemberCount(),
        club.getMaxMembers(),
        status,
        statusLabel(status),
        toRegistrationResponse(registration));
  }

  private StudentClubDetailResponse.Registration toRegistrationResponse(ClubRegistrationEntity registration) {
    if (registration == null) {
      return new StudentClubDetailResponse.Registration(false, null, null);
    }
    boolean registered = registration.getStatus() == ClubRegistrationStatus.PENDING
        || registration.getStatus() == ClubRegistrationStatus.JOINED;
    return new StudentClubDetailResponse.Registration(
        registered,
        toOffsetDateTime(registration.getRegisteredAt()),
        toOffsetDateTime(registration.getApprovedAt()));
  }

  private String status(ClubEntity club, ClubRegistrationEntity registration) {
    if (registration != null
        && registration.getStatus() != null
        && registration.getStatus() != ClubRegistrationStatus.LEFT) {
      return registration.getStatus().name();
    }
    return openOrClosedStatus(club);
  }

  private String openOrClosedStatus(ClubEntity club) {
    return isOpenForRegistration(club) ? STATUS_OPEN : STATUS_CLOSED;
  }

  private boolean isOpenForRegistration(ClubEntity club) {
    return club.isRegistrationOpen()
        && (club.getMaxMembers() == null || club.getMemberCount() < club.getMaxMembers());
  }

  private String statusLabel(String status) {
    return switch (status) {
      case STATUS_OPEN -> "Đang mở";
      case "JOINED" -> "Đang tham gia";
      case "PENDING" -> "Chờ duyệt";
      case STATUS_CLOSED -> "Đã đóng";
      case "REJECTED" -> "Bị từ chối";
      case "LEFT" -> "Đã rời CLB";
      default -> status;
    };
  }

  private String scheduleLabel(ClubEntity club) {
    if (club.getWeekday() == null || club.getStartTime() == null || club.getEndTime() == null) {
      return null;
    }
    return club.getWeekday() + " · " + formatTime(club.getStartTime()) + " - " + formatTime(club.getEndTime());
  }

  private String formatTime(LocalTime time) {
    return time == null ? null : time.format(TIME_FORMATTER);
  }

  private String normalizeReason(ClubRegistrationRequest request) {
    if (request == null || request.reason() == null || request.reason().isBlank()) {
      return null;
    }
    return request.reason().trim();
  }

  private OffsetDateTime toOffsetDateTime(Instant instant) {
    return instant == null ? null : OffsetDateTime.ofInstant(instant, CLUB_ZONE);
  }
}
