package com.fschool.edu.fschool_backend.infrastructure.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final List<String> allowedOriginPatterns;
    private final String studentRequestUploadLocation;

    public CorsConfig(
            @Value("${app.cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*}")
                    List<String> allowedOriginPatterns,
            @Value("${app.upload.student-request-dir:uploads/student-requests}") String studentRequestUploadDir) {
        this.allowedOriginPatterns = allowedOriginPatterns;
        this.studentRequestUploadLocation = uploadLocation(studentRequestUploadDir);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOriginPatterns.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "Content-Disposition")
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/student-requests/**")
                .addResourceLocations(studentRequestUploadLocation);
    }

    private String uploadLocation(String uploadDir) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        String location = uploadPath.toUri().toString();
        return location.endsWith("/") ? location : location + "/";
    }
}
