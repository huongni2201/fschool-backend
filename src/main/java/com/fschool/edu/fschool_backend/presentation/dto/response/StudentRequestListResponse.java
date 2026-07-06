package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record StudentRequestListResponse(
        List<Item> items,
        Pagination pagination) {

    public record Item(
            String id,
            String typeCode,
            String typeName,
            String title,
            String status,
            String statusLabel,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
    }

    public record Pagination(
            int page,
            int limit,
            long total,
            int totalPages) {
    }
}
