package com.fschool.edu.fschool_backend.application.command;

public record ResetPasswordCommand(String phoneNumber, String otp, String resetToken, String newPassword) {
}
