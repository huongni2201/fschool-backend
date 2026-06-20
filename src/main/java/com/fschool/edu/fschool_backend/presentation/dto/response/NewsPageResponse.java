package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.util.List;

public record NewsPageResponse(
        List<NewsListItemResponse> items,
        int page,
        int size,
        long totalItems,
        int totalPages) {
}
