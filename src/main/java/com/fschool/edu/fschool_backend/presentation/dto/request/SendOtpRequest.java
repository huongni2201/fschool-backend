package com.fschool.edu.fschool_backend.presentation.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fschool.edu.fschool_backend.application.command.SendOtpCommand;

public record SendOtpRequest(@JsonAlias("phone") String phoneNumber) {

    public SendOtpCommand toCommand() {
        return new SendOtpCommand(phoneNumber);
    }
}
