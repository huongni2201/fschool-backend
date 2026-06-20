package com.fschool.edu.fschool_backend.presentation.dto.request;

import com.fschool.edu.fschool_backend.application.command.ResetPasswordCommand;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(@NotBlank String resetToken, @NotBlank String newPassword) {

    public ResetPasswordCommand toCommand() {
        return new ResetPasswordCommand(resetToken, newPassword);
    }
}
