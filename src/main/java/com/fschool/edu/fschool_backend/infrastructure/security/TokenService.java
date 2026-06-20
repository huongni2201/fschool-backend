package com.fschool.edu.fschool_backend.infrastructure.security;

import com.fschool.edu.fschool_backend.presentation.exception.ApiException;
import com.fschool.edu.fschool_backend.domain.enums.UserRole;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final byte[] secret;
    private final long accessTokenTtlSeconds;

    public TokenService(
            @Value("${app.security.jwt-secret:fschool-local-development-secret-change-me}") String secret,
            @Value("${app.security.access-token-ttl-seconds:3600}") long accessTokenTtlSeconds) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
    }

    public String createAccessToken(UUID userId, UserRole role) {
        long expiresAt = Instant.now().plusSeconds(accessTokenTtlSeconds).getEpochSecond();
        String payload = userId + ":" + role.name() + ":" + expiresAt;
        return encode(payload) + "." + sign(payload);
    }

    public CurrentUser requireUser(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Missing bearer token");
        }
        return parse(authorizationHeader.substring("Bearer ".length()));
    }

    public long accessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }

    private CurrentUser parse(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
        String payload = decode(parts[0]);
        if (!sign(payload).equals(parts[1])) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid token signature");
        }
        String[] values = payload.split(":");
        if (values.length != 3) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid token payload");
        }
        long expiresAt = Long.parseLong(values[2]);
        if (Instant.now().getEpochSecond() >= expiresAt) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Token has expired");
        }
        return new CurrentUser(UUID.fromString(values[0]), UserRole.valueOf(values[1]));
    }

    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot sign token", exception);
        }
    }
}
