package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

    private final String accessToken;
    private final String refreshToken;
    private final String tokenType;
    private final long expiresIn;
    private final String role;
    private final UUID userId;
    private final String studentCode;
    private final String fullName;
    private final String className;
}
