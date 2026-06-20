package com.fschool.edu.fschool_backend.presentation.controller;

import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.NewsDetailResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.NewsPageResponse;
import com.fschool.edu.fschool_backend.application.service.StudentPortalService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/news")
public class NewsController {

    private final StudentPortalService portalService;

    public NewsController(StudentPortalService portalService) {
        this.portalService = portalService;
    }

    @GetMapping
    ApiResponse<NewsPageResponse> news(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(portalService.getNews(page, size));
    }

    @GetMapping("/{id}")
    ApiResponse<NewsDetailResponse> detail(@PathVariable UUID id) {
        return ApiResponse.ok(portalService.getNewsDetail(id));
    }
}
