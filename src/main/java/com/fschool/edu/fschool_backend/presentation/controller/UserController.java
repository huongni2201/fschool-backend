package com.fschool.edu.fschool_backend.presentation.controller;

import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import com.fschool.edu.fschool_backend.presentation.dto.request.UpdateMeRequest;
import com.fschool.edu.fschool_backend.presentation.dto.response.UserMeResponse;
import com.fschool.edu.fschool_backend.infrastructure.security.TokenService;
import com.fschool.edu.fschool_backend.application.service.StudentPortalService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final TokenService tokenService;
    private final StudentPortalService portalService;

    public UserController(TokenService tokenService, StudentPortalService portalService) {
        this.tokenService = tokenService;
        this.portalService = portalService;
    }

    @GetMapping("/me")
    ApiResponse<UserMeResponse> me(@RequestHeader(name = "Authorization", required = false) String authorization) {
        return ApiResponse.ok(portalService.getMe(tokenService.requireUser(authorization).id()));
    }

    @PatchMapping("/me")
    ApiResponse<UserMeResponse> updateMe(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestBody UpdateMeRequest request) {
        return ApiResponse.ok(portalService.updateMe(tokenService.requireUser(authorization).id(), request.toCommand()));
    }
}
