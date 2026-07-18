package com.fschool.edu.fschool_backend.presentation.dto.response;

import java.util.List;

public record TeacherClassStudentsResponse(
        ClassInfo classInfo,
        List<Student> students) {

    public record ClassInfo(
            String id,
            String name,
            String subjectName,
            long studentCount) {
    }

    public record Student(
            String id,
            String studentCode,
            String fullName,
            String className,
            String statusLabel) {
    }
}
