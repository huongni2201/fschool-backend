package com.fschool.edu.fschool_backend.presentation.dto.request;

import com.fschool.edu.fschool_backend.presentation.dto.response.NotificationSettingsResponse;

public record NotificationSettingsRequest(
        Boolean academic,
        Boolean finance,
        Boolean request,
        Boolean event) {

    public NotificationSettingsResponse mergeWith(NotificationSettingsResponse current) {
        return new NotificationSettingsResponse(
                academic == null ? current.academic() : academic,
                finance == null ? current.finance() : finance,
                request == null ? current.request() : request,
                event == null ? current.event() : event);
    }
}
