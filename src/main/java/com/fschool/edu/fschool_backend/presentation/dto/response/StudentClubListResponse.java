package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.util.List;

public record StudentClubListResponse(List<Item> items) {

    public record Item(
            String id,
            String code,
            String name,
            String description,
            String teacherName,
            String location,
            String scheduleLabel,
            int memberCount,
            String status,
            String statusLabel) {
    }
}
