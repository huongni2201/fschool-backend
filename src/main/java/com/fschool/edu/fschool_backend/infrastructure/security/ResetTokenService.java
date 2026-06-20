package com.fschool.edu.fschool_backend.infrastructure.security;

import com.fschool.edu.fschool_backend.presentation.exception.ApiException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ResetTokenService {

    private final Map<String, ResetToken> tokens = new ConcurrentHashMap<>();

    public String create(UUID userId) {
        String token = UUID.randomUUID() + "." + UUID.randomUUID();
        tokens.put(token, new ResetToken(userId, Instant.now().plusSeconds(600)));
        return token;
    }

    public UUID consume(String token) {
        ResetToken resetToken = tokens.remove(token);
        if (resetToken == null || Instant.now().isAfter(resetToken.expiresAt())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Reset token is invalid or expired");
        }
        return resetToken.userId();
    }

    private record ResetToken(UUID userId, Instant expiresAt) {
    }
}
