package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.util.UUID;

public record CurrentUserResponse(
        UUID userId,
        String username,
        String fullName,
        String role,
        String studentCode,
        String className) {
}
