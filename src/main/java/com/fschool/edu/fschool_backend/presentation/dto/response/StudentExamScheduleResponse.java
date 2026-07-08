package com.fschool.edu.fschool_backend.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StudentExamScheduleResponse(
        String termName,
        Instant lastUpdatedAt,
        List<ExamItem> exams) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ExamItem(
            String id,
            String subjectName,
            String examType,
            LocalDate examDate,
            String startTime,
            String endTime,
            Integer durationMinutes,
            String roomName,
            String seatNumber,
            String form,
            String status,
            String statusLabel,
            String note) {
    }
}
