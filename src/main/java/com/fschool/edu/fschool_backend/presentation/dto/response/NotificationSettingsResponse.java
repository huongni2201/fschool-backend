package com.fschool.edu.fschool_backend.presentation.dto.response;

public record NotificationSettingsResponse(
        boolean academic,
        boolean finance,
        boolean request,
        boolean event) {

    public static NotificationSettingsResponse defaults() {
        return new NotificationSettingsResponse(true, true, true, true);
    }
}
