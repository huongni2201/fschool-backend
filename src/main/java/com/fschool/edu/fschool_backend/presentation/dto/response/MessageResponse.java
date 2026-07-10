package com.fschool.edu.fschool_backend.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"success", "message"})
public record MessageResponse(boolean success, String message) {

    public static MessageResponse ok(String message) {
        return new MessageResponse(true, message);
    }

    public static MessageResponse error(String message) {
        return new MessageResponse(false, message);
    }
}
