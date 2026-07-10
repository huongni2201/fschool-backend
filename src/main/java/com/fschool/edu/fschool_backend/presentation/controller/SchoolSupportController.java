package com.fschool.edu.fschool_backend.presentation.controller;

import com.fschool.edu.fschool_backend.infrastructure.security.TokenService;
import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.SupportInfoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/school/support-info")
public class SchoolSupportController {

    private final TokenService tokenService;
    private final SupportInfoResponse supportInfo;

    public SchoolSupportController(
            TokenService tokenService,
            @Value("${app.school.support.hotline:1900 6600}") String hotline,
            @Value("${app.school.support.email:support@fptschools.edu.vn}") String email,
            @Value("${app.school.support.office:Li\u00EAn h\u1EC7 gi\u00E1o vi\u00EAn ch\u1EE7 nhi\u1EC7m ho\u1EB7c v\u0103n ph\u00F2ng c\u01A1 s\u1EDF}") String office) {
        this.tokenService = tokenService;
        this.supportInfo = new SupportInfoResponse(hotline, email, office);
    }

    @GetMapping
    ApiResponse<SupportInfoResponse> supportInfo(
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        tokenService.requireUser(authorization);
        return ApiResponse.ok(supportInfo);
    }
}
