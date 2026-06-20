package com.fschool.edu.fschool_backend.application.command;

public record ResetPasswordCommand(String resetToken, String newPassword) {
}
