package com.fschool.edu.fschool_backend.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StudentProfileResponse(
        Student student,
        List<Parent> parents) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Student(
            String fullName,
            String studentCode,
            String avatarText,
            String className,
            String campus,
            String schoolYear,
            String phone,
            String email,
            LocalDate dateOfBirth,
            String gender,
            String address) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Parent(
            String name,
            String relation,
            String phone,
            String email,
            String address) {
    }
}
