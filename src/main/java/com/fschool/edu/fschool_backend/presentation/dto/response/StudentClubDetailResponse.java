package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.time.OffsetDateTime;

public record StudentClubDetailResponse(
        String id,
        String code,
        String name,
        String description,
        Teacher teacher,
        String location,
        Schedule schedule,
        int memberCount,
        Integer maxMembers,
        String status,
        String statusLabel,
        Registration registration) {

    public record Teacher(
            String id,
            String name,
            String phone,
            String email) {
    }

    public record Schedule(
            String weekday,
            String startTime,
            String endTime) {
    }

    public record Registration(
            boolean registered,
            OffsetDateTime registeredAt,
            OffsetDateTime approvedAt) {
    }
}
