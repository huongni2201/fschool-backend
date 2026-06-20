package com.fschool.edu.fschool_backend.presentation.controller;

import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import com.fschool.edu.fschool_backend.presentation.dto.request.LoginRequest;
import com.fschool.edu.fschool_backend.presentation.dto.request.ResetPasswordRequest;
import com.fschool.edu.fschool_backend.presentation.dto.request.SendOtpRequest;
import com.fschool.edu.fschool_backend.presentation.dto.request.VerifyOtpRequest;
import com.fschool.edu.fschool_backend.presentation.dto.response.LoginResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.SendOtpResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.VerifyOtpResponse;
import com.fschool.edu.fschool_backend.application.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request.toCommand()));
    }

    @PostMapping("/forgot-password/send-otp")
    ApiResponse<SendOtpResponse> sendForgotPasswordOtp(@Valid @RequestBody SendOtpRequest request) {
        return ApiResponse.ok(authService.sendForgotPasswordOtp(request.toCommand()));
    }

    @PostMapping("/forgot-password/verify-otp")
    ApiResponse<VerifyOtpResponse> verifyForgotPasswordOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ApiResponse.ok(authService.verifyForgotPasswordOtp(request.toCommand()));
    }

    @PostMapping("/forgot-password/reset")
    ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.toCommand());
        return ApiResponse.ok();
    }

    @PostMapping("/logout")
    ApiResponse<Void> logout() {
        return ApiResponse.ok();
    }
}
