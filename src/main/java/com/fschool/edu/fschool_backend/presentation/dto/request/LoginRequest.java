package com.fschool.edu.fschool_backend.presentation.dto.request;

import com.fschool.edu.fschool_backend.application.command.LoginCommand;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String username, @NotBlank String password) {

    public LoginCommand toCommand() {
        return new LoginCommand(username, password);
    }
}
