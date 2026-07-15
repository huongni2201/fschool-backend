package com.fschool.edu.fschool_backend.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fschool.edu.fschool_backend.domain.enums.UserStatus;
import com.fschool.edu.fschool_backend.infrastructure.config.JwtConfig;
import com.fschool.edu.fschool_backend.infrastructure.config.SecurityConfig;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.SignedJWT;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtEncoder;

class JwtServiceTest {

    private static final String TEST_ISSUER = "fschool-test";
    private static final Instant ISSUED_AT = Instant.parse("2026-07-14T00:00:00Z");

    @Test
    void generateAccessTokenReturnsRs256Jwt() throws Exception {
        JwtService jwtService = jwtService(Duration.ofHours(1), Duration.ofDays(30), Clock.fixed(ISSUED_AT, ZoneOffset.UTC));
        CustomUserDetails user = user(UUID.randomUUID(), "STUDENT");

        String token = jwtService.generateAccessToken(user);

        assertEquals(3, token.split("\\.").length);
        SignedJWT jwt = SignedJWT.parse(token);
        assertEquals(JWSAlgorithm.RS256, jwt.getHeader().getAlgorithm());
        assertEquals(JOSEObjectType.JWT, jwt.getHeader().getType());
        assertEquals(TEST_ISSUER, jwt.getJWTClaimsSet().getIssuer());
        assertEquals(user.getId().toString(), jwt.getJWTClaimsSet().getSubject());
        assertEquals(user.getUsername(), jwt.getJWTClaimsSet().getStringClaim("username"));
        assertEquals(user.getUsername(), jwt.getJWTClaimsSet().getStringClaim("phone"));
        assertEquals("student", jwt.getJWTClaimsSet().getStringClaim("role"));
        assertEquals(List.of("STUDENT"), jwt.getJWTClaimsSet().getStringListClaim("roles"));
        assertEquals("access", jwt.getJWTClaimsSet().getStringClaim("token_use"));
        assertTrue(jwt.getJWTClaimsSet().getExpirationTime().after(jwt.getJWTClaimsSet().getIssueTime()));
    }

    @Test
    void jwtAuthenticationConverterUsesJwtPrincipalAndRoleAuthorities() {
        JwtService jwtService = jwtService(Duration.ofHours(1), Duration.ofDays(30), Clock.systemUTC());
        Jwt jwt = jwtDecoder().decode(jwtService.generateAccessToken(user(UUID.randomUUID(), "PARENT")));

        AbstractAuthenticationToken authentication =
                new SecurityConfig().jwtAuthenticationConverter().convert(jwt);

        assertEquals(jwt.getSubject(), authentication.getName());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_PARENT".equals(authority.getAuthority())));
    }

    @Test
    void jwtAuthenticationConverterPrefixesRolesForSpringSecurity() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuer(TEST_ISSUER)
                .subject(userId.toString())
                .claim("username", "0900000000")
                .claim("roles", List.of("PARENT"))
                .claim("token_use", "access")
                .build();

        AbstractAuthenticationToken authentication =
                new SecurityConfig().jwtAuthenticationConverter().convert(jwt);

        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_PARENT".equals(authority.getAuthority())));
    }

    @Test
    void decoderRejectsRefreshTokenForResourceServer() {
        JwtService jwtService = jwtService(Duration.ofHours(1), Duration.ofDays(30), Clock.systemUTC());
        String token = jwtService.generateRefreshToken(user(UUID.randomUUID(), "PARENT"));

        assertThrows(JwtException.class, () -> jwtDecoder().decode(token));
    }

    @Test
    void decoderRejectsExpiredAccessToken() {
        JwtService jwtService = jwtService(
                Duration.ofSeconds(1),
                Duration.ofDays(30),
                Clock.fixed(Instant.parse("2000-01-01T00:00:00Z"), ZoneOffset.UTC));
        String token = jwtService.generateAccessToken(user(UUID.randomUUID(), "STUDENT"));

        assertThrows(JwtException.class, () -> jwtDecoder().decode(token));
    }

    private JwtService jwtService(Duration accessTokenTtl, Duration refreshTokenTtl, Clock clock) {
        JwtProperties properties = jwtProperties(accessTokenTtl, refreshTokenTtl);
        JwtConfig jwtConfig = new JwtConfig();
        RSAPublicKey publicKey = jwtConfig.rsaPublicKey(properties);
        RSAPrivateKey privateKey = jwtConfig.rsaPrivateKey(properties);
        JwtEncoder jwtEncoder = jwtConfig.jwtEncoder(publicKey, privateKey);
        return new JwtService(jwtEncoder, properties, clock);
    }

    private JwtDecoder jwtDecoder() {
        JwtProperties properties = jwtProperties(Duration.ofHours(1), Duration.ofDays(30));
        JwtConfig jwtConfig = new JwtConfig();
        RSAPublicKey publicKey = jwtConfig.rsaPublicKey(properties);
        return jwtConfig.jwtDecoder(publicKey, properties);
    }

    private JwtProperties jwtProperties(Duration accessTokenTtl, Duration refreshTokenTtl) {
        JwtProperties properties = new JwtProperties();
        properties.setIssuer(TEST_ISSUER);
        properties.setAccessTokenTtl(accessTokenTtl);
        properties.setRefreshTokenTtl(refreshTokenTtl);
        return properties;
    }

    private CustomUserDetails user(UUID id, String role) {
        return CustomUserDetails.builder()
                .id(id)
                .username("0900000000")
                .password("{noop}123456")
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();
    }
}
