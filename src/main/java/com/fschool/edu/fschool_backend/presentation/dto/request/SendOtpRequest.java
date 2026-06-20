package com.fschool.edu.fschool_backend.presentation.dto.request;

import com.fschool.edu.fschool_backend.application.command.SendOtpCommand;
import jakarta.validation.constraints.NotBlank;

public record SendOtpRequest(@NotBlank String phone) {

    public SendOtpCommand toCommand() {
        return new SendOtpCommand(phone);
    }
}
