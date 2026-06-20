package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.util.UUID;

public record ClassSummaryResponse(
        UUID id,
        String name,
        Short gradeNumber,
        String roomName,
        String homeroomTeacherName) {
}
