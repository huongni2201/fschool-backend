package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.util.List;

public record RequestTypeResponse(
        String code,
        String name,
        String description,
        String iconName,
        boolean requiresDateRange,
        boolean requiresAttachment,
        List<Field> fields) {

    public record Field(
            String key,
            String label,
            String type,
            boolean required) {
    }
}
