package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.time.LocalTime;
import java.util.UUID;

public record LessonResponse(
        UUID id,
        Short periodNo,
        String subjectCode,
        String subjectName,
        LocalTime startTime,
        LocalTime endTime,
        String teacherName,
        String roomName) {
}
