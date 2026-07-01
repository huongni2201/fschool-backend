package com.fschool.edu.fschool_backend.application.command;

import com.fschool.edu.fschool_backend.domain.enums.Gender;
import java.time.LocalDate;
import java.util.UUID;

public record RegisterCommand(
        String phone,
        String password,
        String studentCode,
        String fullName,
        UUID classId,
        LocalDate dateOfBirth,
        Gender gender,
        String avatarUrl,
        String address,
        String guardianName,
        String guardianPhone) {
}
