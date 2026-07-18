package com.fschool.edu.fschool_backend.presentation.dto.response;

import com.fschool.edu.fschool_backend.domain.enums.Gender;
import java.time.LocalDate;
import java.util.UUID;

public record AdminStudentResponse(
        UUID id,
        String studentCode,
        String fullName,
        UUID classId,
        String className,
        LocalDate dateOfBirth,
        Gender gender,
        String contactPhoneNumber,
        String status) {
}
