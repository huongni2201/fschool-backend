package com.fschool.edu.fschool_backend.presentation.controller;

import com.fschool.edu.fschool_backend.infrastructure.config.SchoolSupportProperties;
import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.SupportInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/school/support-info")
@RequiredArgsConstructor
public class SchoolSupportController {

    private final SchoolSupportProperties properties;

    @GetMapping
    ApiResponse<SupportInfoResponse> supportInfo() {
        return ApiResponse.ok(new SupportInfoResponse(
                properties.getHotline(),
                properties.getEmail(),
                properties.getOffice()));
    }
}
