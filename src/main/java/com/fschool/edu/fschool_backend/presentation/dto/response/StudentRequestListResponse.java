package com.fschool.edu.fschool_backend.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StudentRequestListResponse(
        List<Item> items,
        int page,
        int limit,
        long total,
        int totalPages,
        Pagination pagination) {

    public record Item(
            String id,
            String typeCode,
            String typeName,
            String title,
            String status,
            String statusLabel,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record Pagination(
            int page,
            int limit,
            long total,
            int totalPages) {
    }
}
