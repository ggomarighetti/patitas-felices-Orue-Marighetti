package com.patitas.gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String TEST_SECRET = "test-only-verifier-key-0123456789abcdef0123456789abcdef0123456789abcdef";
    private final JwtService service = new JwtService(TEST_SECRET);

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"falso", "a.b.c"})
    void malformedInputIsRejected(String token) {
        assertFalse(service.validateToken(token));
    }

    @Test
    void anotherHmacAlgorithmCannotAuthenticateEvenWithTheCorrectKey() {
        String token = Jwts.builder().subject("jorge").claim("rol", "ADMIN").issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS512).compact();

        assertFalse(service.validateToken(token));
    }

    @Test
    void missingRoleAndExpirationCannotAuthenticate() {
        String token = Jwts.builder().subject("jorge").issuedAt(new Date())
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256).compact();

        assertFalse(service.validateToken(token));
    }

    @Test
    void numericRoleDoesNotCauseAnInternalError() {
        String token = Jwts.builder().subject("jorge").claim("rol", 123).issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256).compact();

        assertFalse(service.validateToken(token));
    }
}
