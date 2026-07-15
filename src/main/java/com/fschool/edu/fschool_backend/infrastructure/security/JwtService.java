package com.fschool.edu.fschool_backend.infrastructure.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    public String generateAccessToken(CustomUserDetails user) {
        return generateToken(user, jwtProperties.getAccessTokenTtl(), "access");
    }

    public String generateRefreshToken(CustomUserDetails user) {
        return generateToken(user, jwtProperties.getRefreshTokenTtl(), "refresh");
    }

    public long accessTokenTtlSeconds() {
        return jwtProperties.getAccessTokenTtl().toSeconds();
    }

    private String generateToken(CustomUserDetails user, Duration ttl, String tokenUse) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalStateException("JWT token TTL must be positive");
        }
        Instant now = Instant.now(clock);
        List<String> roles = roles(user);
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .id(UUID.randomUUID().toString())
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .claim("phone", user.getUsername())
                .claim("roles", roles)
                .claim("token_use", tokenUse);
        if (!roles.isEmpty()) {
            claims.claim("role", roles.getFirst().toLowerCase(Locale.ROOT));
        }

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
                .type("JWT")
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims.build()))
                .getTokenValue();
    }

    private List<String> roles(CustomUserDetails user) {
        if (user.getRole() == null || user.getRole().isBlank()) {
            return List.of();
        }
        return List.of(user.getRole().trim().toUpperCase(Locale.ROOT));
    }
}
