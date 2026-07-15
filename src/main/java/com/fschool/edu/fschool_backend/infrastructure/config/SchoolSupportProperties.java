package com.fschool.edu.fschool_backend.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.school.support")
public class SchoolSupportProperties {

    private String hotline = "1900 6600";
    private String email = "support@fptschools.edu.vn";
    private String office = "Lien he giao vien chu nhiem hoac van phong co so";
}
