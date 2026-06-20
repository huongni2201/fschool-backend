package com.fschool.edu.fschool_backend.application.command;

import java.util.UUID;

public record VerifyOtpCommand(UUID challengeId, String otp) {
}
