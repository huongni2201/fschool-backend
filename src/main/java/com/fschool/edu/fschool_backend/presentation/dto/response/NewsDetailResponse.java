package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.time.Instant;
import java.util.UUID;

public record NewsDetailResponse(
        UUID id,
        String title,
        String summary,
        String content,
        String thumbnailUrl,
        Instant publishedAt) {
}
