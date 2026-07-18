package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.util.UUID;

public record SubjectFilterResponse(
        UUID id,
        String code,
        String name) {
}
