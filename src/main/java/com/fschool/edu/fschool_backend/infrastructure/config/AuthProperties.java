package com.fschool.edu.fschool_backend.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private String devOtpCode = "123456";
    private long otpTtlSeconds = 300;
    private long otpResendCooldownSeconds = 60;
}
