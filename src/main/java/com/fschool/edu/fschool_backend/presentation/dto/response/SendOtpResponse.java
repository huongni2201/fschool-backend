package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.util.UUID;

public record SendOtpResponse(UUID challengeId, long expiresIn) {
}
