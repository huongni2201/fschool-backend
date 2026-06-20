package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.util.UUID;

public record CurrentSemesterResponse(UUID id, String name, Short semesterNo, String schoolYear, Boolean isCurrent) {
}
