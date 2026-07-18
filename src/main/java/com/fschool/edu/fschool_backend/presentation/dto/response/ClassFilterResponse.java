package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.util.UUID;

public record ClassFilterResponse(
        UUID id,
        String name,
        int gradeLevel) {
}
