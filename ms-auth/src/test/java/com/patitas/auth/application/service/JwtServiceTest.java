package com.patitas.auth.application.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String TEST_SECRET = "test-only-signing-key-0123456789abcdef0123456789abcdef0123456789abcdef";
    private final JwtService service = new JwtService(TEST_SECRET, 86400000);

    @Test
    void issuedTokenUsesHs256AndCarriesIdentityAndTwentyFourHourExpiration() {
        String token = service.generateToken("jorge", "ADMIN");
        var signed = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)))
                .build().parseSignedClaims(token);
        Claims claims = signed.getPayload();

        assertEquals("HS256", signed.getHeader().getAlgorithm());
        assertEquals("jorge", claims.getSubject());
        assertEquals("ADMIN", claims.get("rol", String.class));
        assertEquals(86400000, claims.getExpiration().getTime() - claims.getIssuedAt().getTime());
        assertTrue(service.validateToken(token));
        assertEquals("jorge", service.extractUsername(token));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"falso", "a.b.c", "eyJhbGcTokenFalso"})
    void malformedTokensAreRejectedWithoutPropagatingParserErrors(String token) {
        assertFalse(service.validateToken(token));
    }

    @Test
    void expiredTokenIsRejected() {
        String token = Jwts.builder().subject("jorge").claim("rol", "ADMIN")
                .issuedAt(new Date(0)).expiration(new Date(1000))
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256).compact();

        assertFalse(service.validateToken(token));
    }

    @Test
    void anotherSigningKeyCannotAuthenticate() {
        String token = new JwtService(TEST_SECRET + "different", 86400000).generateToken("jorge", "ADMIN");

        assertFalse(service.validateToken(token));
    }

    @Test
    void tokenWithoutExpirationIsRejected() {
        String token = Jwts.builder().subject("jorge").claim("rol", "ADMIN").issuedAt(new Date())
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256).compact();

        assertFalse(service.validateToken(token));
    }

    @Test
    void weakConfigurationFailsBeforeTheServiceCanIssueTokens() {
        assertThrows(WeakKeyException.class, () -> new JwtService("corta", 86400000));
        assertThrows(IllegalArgumentException.class, () -> new JwtService(TEST_SECRET, 0));
    }
}
