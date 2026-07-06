package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record StudentRequestDetailResponse(
        String id,
        String typeCode,
        String typeName,
        String title,
        String status,
        String statusLabel,
        Student student,
        Map<String, Object> formData,
        List<Attachment> attachments,
        List<History> history,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public record Student(
            String id,
            String name,
            String className) {
    }

    public record Attachment(
            String id,
            String fileName,
            String url,
            String mimeType,
            long size) {
    }

    public record History(
            String status,
            String statusLabel,
            String note,
            OffsetDateTime createdAt) {
    }
}
