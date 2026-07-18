package com.fschool.edu.fschool_backend.infrastructure.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.upload")
public class StudentRequestUploadProperties {

    private String studentRequestDir = "uploads/student-requests";
    private String studentRequestPublicPath = "/uploads/student-requests";
    private String timetableDir = "uploads/timetables";
    private String timetablePublicPath = "/uploads/timetables";

    public Path studentRequestDirPath() {
        return Paths.get(studentRequestDir).toAbsolutePath().normalize();
    }

    public Path timetableDirPath() {
        return Paths.get(timetableDir).toAbsolutePath().normalize();
    }
}
