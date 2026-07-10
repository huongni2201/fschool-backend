package com.fschool.edu.fschool_backend.presentation.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;
import java.util.Map;

public record CreateStudentRequestRequest(
        @JsonAlias("typeCode")
        String requestTypeCode,
        String title,
        String content,
        String startDate,
        String endDate,
        @JsonAlias("formData")
        Map<String, Object> fields,
        List<String> attachmentIds) {

    public String typeCode() {
        return requestTypeCode;
    }

    public Map<String, Object> formData() {
        return fields;
    }
}
