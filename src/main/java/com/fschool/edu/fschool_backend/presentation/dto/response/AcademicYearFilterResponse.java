package com.fschool.edu.fschool_backend.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record AcademicYearFilterResponse(
        UUID id,
        String name,
        @JsonProperty("isCurrent") boolean current) {
}
