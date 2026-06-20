package com.fschool.edu.fschool_backend.presentation.dto.response;

import com.fschool.edu.fschool_backend.domain.enums.AssignmentStatus;
import java.time.Instant;
import java.util.UUID;

public record AssignmentResponse(
        UUID id,
        String title,
        String description,
        String subjectName,
        String teacherName,
        String attachmentUrl,
        Instant dueAt,
        AssignmentStatus status,
        boolean isOverdue) {
}
