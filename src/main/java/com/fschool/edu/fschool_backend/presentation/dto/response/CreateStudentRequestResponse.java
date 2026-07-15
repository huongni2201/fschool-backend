package com.fschool.edu.fschool_backend.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Locale;

public record CreateStudentRequestResponse(
        String id,
        String typeCode,
        String typeName,
        String title,
        String status,
        String statusLabel,
        Instant createdAt,
        Instant updatedAt) {

    @JsonProperty("requestTypeCode")
    public String requestTypeCode() {
        return typeCode == null ? null : typeCode.toUpperCase(Locale.ROOT);
    }
}
