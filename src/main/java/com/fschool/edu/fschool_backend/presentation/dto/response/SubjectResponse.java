package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.util.UUID;

public record SubjectResponse(UUID id, String code, String name, Boolean scoreBased) {
}
