package com.fschool.edu.fschool_backend.presentation.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fschool.edu.fschool_backend.application.command.VerifyOtpCommand;
import java.util.UUID;

public record VerifyOtpRequest(
        @JsonAlias("phone") String phoneNumber,
        String otp,
        UUID challengeId) {

    public VerifyOtpCommand toCommand() {
        return new VerifyOtpCommand(challengeId, phoneNumber, otp);
    }
}
