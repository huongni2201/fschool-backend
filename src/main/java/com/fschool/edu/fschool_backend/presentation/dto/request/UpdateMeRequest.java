package com.fschool.edu.fschool_backend.presentation.dto.request;

import com.fschool.edu.fschool_backend.application.command.UpdateMeCommand;

public record UpdateMeRequest(String avatarUrl, String address) {

    public UpdateMeCommand toCommand() {
        return new UpdateMeCommand(avatarUrl, address);
    }
}
