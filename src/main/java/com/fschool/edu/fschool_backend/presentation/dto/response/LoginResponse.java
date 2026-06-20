package com.fschool.edu.fschool_backend.presentation.dto.response;

public record LoginResponse(String accessToken, String tokenType, long expiresIn, LoginUserResponse user) {
}
