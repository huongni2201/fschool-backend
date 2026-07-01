package com.fschool.edu.fschool_backend.presentation.dto.request;

import com.fschool.edu.fschool_backend.application.command.RegisterCommand;
import com.fschool.edu.fschool_backend.domain.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record RegisterRequest(
        @NotBlank @Size(max = 20) String phone,
        @NotBlank @Size(min = 6, max = 100) String password,
        @NotBlank @Size(max = 20) String studentCode,
        @NotBlank @Size(max = 150) String fullName,
        UUID classId,
        LocalDate dateOfBirth,
        Gender gender,
        String avatarUrl,
        @Size(max = 255) String address,
        @Size(max = 150) String guardianName,
        @Size(max = 20) String guardianPhone) {

    public RegisterCommand toCommand() {
        return new RegisterCommand(
                phone,
                password,
                studentCode,
                fullName,
                classId,
                dateOfBirth,
                gender,
                avatarUrl,
                address,
                guardianName,
                guardianPhone);
    }
}
