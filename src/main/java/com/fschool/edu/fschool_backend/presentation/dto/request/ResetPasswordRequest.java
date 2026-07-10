package com.fschool.edu.fschool_backend.presentation.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fschool.edu.fschool_backend.application.command.ResetPasswordCommand;

public record ResetPasswordRequest(
        @JsonAlias("phone") String phoneNumber,
        String otp,
        String resetToken,
        String newPassword) {

    public ResetPasswordCommand toCommand() {
        return new ResetPasswordCommand(phoneNumber, otp, resetToken, newPassword);
    }
}
