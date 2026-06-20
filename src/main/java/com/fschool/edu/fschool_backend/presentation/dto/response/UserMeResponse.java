package com.fschool.edu.fschool_backend.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fschool.edu.fschool_backend.domain.enums.Gender;
import java.time.LocalDate;
import java.util.UUID;

public record UserMeResponse(
        UUID id,
        String phone,
        String studentCode,
        String fullName,
        LocalDate dateOfBirth,
        Gender gender,
        String avatarUrl,
        String address,
        String guardianName,
        String guardianPhone,
        @JsonProperty("class")
        ClassSummaryResponse clazz) {
}
