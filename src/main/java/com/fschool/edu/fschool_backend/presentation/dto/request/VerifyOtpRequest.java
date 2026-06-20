package com.fschool.edu.fschool_backend.presentation.dto.request;

import com.fschool.edu.fschool_backend.application.command.VerifyOtpCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record VerifyOtpRequest(@NotNull UUID challengeId, @NotBlank String otp) {

    public VerifyOtpCommand toCommand() {
        return new VerifyOtpCommand(challengeId, otp);
    }
}
