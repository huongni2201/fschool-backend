package com.fschool.edu.fschool_backend.presentation.dto.request;

import java.util.List;
import java.util.Map;

public record CreateStudentRequestRequest(
        String typeCode,
        String title,
        Map<String, Object> formData,
        List<String> attachmentIds) {
}
