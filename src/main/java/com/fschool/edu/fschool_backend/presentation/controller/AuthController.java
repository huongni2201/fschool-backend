package com.fschool.edu.fschool_backend.presentation.controller;

import com.fschool.edu.fschool_backend.application.service.AuthService;
import com.fschool.edu.fschool_backend.presentation.dto.request.LoginRequest;
import com.fschool.edu.fschool_backend.presentation.dto.request.RegisterRequest;
import com.fschool.edu.fschool_backend.presentation.dto.request.ResetPasswordRequest;
import com.fschool.edu.fschool_backend.presentation.dto.request.SendOtpRequest;
import com.fschool.edu.fschool_backend.presentation.dto.request.VerifyOtpRequest;
import com.fschool.edu.fschool_backend.presentation.dto.response.ApiResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.LoginResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.MessageResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.RegisterResponse;
import com.fschool.edu.fschool_backend.presentation.dto.response.VerifyOtpResponse;
import com.fschool.edu.fschool_backend.presentation.exception.ApiException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String SEND_OTP_SUCCESS_MESSAGE = "M\u00E3 OTP \u0111\u00E3 \u0111\u01B0\u1EE3c g\u1EEDi";
    private static final String SEND_OTP_FAILED_MESSAGE = "Kh\u00F4ng th\u1EC3 g\u1EEDi OTP";
    private static final String VERIFY_OTP_SUCCESS_MESSAGE = "M\u00E3 OTP h\u1EE3p l\u1EC7";
    private static final String VERIFY_OTP_FAILED_MESSAGE =
            "M\u00E3 OTP kh\u00F4ng \u0111\u00FAng ho\u1EB7c \u0111\u00E3 h\u1EBFt h\u1EA1n";
    private static final String RESET_PASSWORD_SUCCESS_MESSAGE =
            "\u0110\u1EB7t l\u1EA1i m\u1EADt kh\u1EA9u th\u00E0nh c\u00F4ng";
    private static final String RESET_PASSWORD_FAILED_MESSAGE =
            "Kh\u00F4ng th\u1EC3 \u0111\u1EB7t l\u1EA1i m\u1EADt kh\u1EA9u";

    private final AuthService authService;

    @PostMapping("/login")
    ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request.toCommand()));
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request.toCommand()));
    }

    @PostMapping("/forgot-password/send-otp")
    ResponseEntity<MessageResponse> sendForgotPasswordOtp(
            @RequestBody(required = false) SendOtpRequest request) {
        try {
            authService.sendForgotPasswordOtp((request == null ? new SendOtpRequest(null) : request).toCommand());
            return ResponseEntity.ok(MessageResponse.ok(SEND_OTP_SUCCESS_MESSAGE));
        } catch (ApiException exception) {
            return ResponseEntity.status(exception.getStatus()).body(MessageResponse.error(exception.getMessage()));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(MessageResponse.error(SEND_OTP_FAILED_MESSAGE));
        }
    }

    @PostMapping("/forgot-password/verify-otp")
    ResponseEntity<?> verifyForgotPasswordOtp(
            @RequestBody(required = false) VerifyOtpRequest request) {
        try {
            VerifyOtpResponse response = authService.verifyForgotPasswordOtp(
                    (request == null ? new VerifyOtpRequest(null, null, null) : request).toCommand());
            return ResponseEntity.ok(ApiResponse.ok(response, VERIFY_OTP_SUCCESS_MESSAGE));
        } catch (ApiException exception) {
            return ResponseEntity.status(exception.getStatus()).body(MessageResponse.error(exception.getMessage()));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(MessageResponse.error(VERIFY_OTP_FAILED_MESSAGE));
        }
    }

    @PostMapping("/forgot-password/reset")
    ResponseEntity<MessageResponse> resetPassword(
            @RequestBody(required = false) ResetPasswordRequest request) {
        try {
            authService.resetPassword((request == null ? new ResetPasswordRequest(null, null, null, null) : request)
                    .toCommand());
            return ResponseEntity.ok(MessageResponse.ok(RESET_PASSWORD_SUCCESS_MESSAGE));
        } catch (ApiException exception) {
            return ResponseEntity.status(exception.getStatus()).body(MessageResponse.error(exception.getMessage()));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(MessageResponse.error(RESET_PASSWORD_FAILED_MESSAGE));
        }
    }

    @PostMapping("/logout")
    ApiResponse<Void> logout() {
        return ApiResponse.ok();
    }
}
