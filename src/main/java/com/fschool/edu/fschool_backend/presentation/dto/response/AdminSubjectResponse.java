package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.util.List;
import java.util.UUID;

public record AdminSubjectResponse(
        UUID id,
        String subjectCode,
        String name,
        List<Integer> gradeLevels,
        int lessonsPerWeek,
        String status) {
}
