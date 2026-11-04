package com.patitas.auth.application.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final JwtParser parser;
    private final long expirationMillis;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration-ms:86400000}") long expirationMillis) {
        if (expirationMillis <= 0) {
            throw new IllegalArgumentException("La duración del token debe ser positiva");
        }
        signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        parser = Jwts.parser().verifyWith(signingKey).build();
        this.expirationMillis = expirationMillis;
    }

    public String generateToken(String username, String rol) {
        Date issuedAt = new Date();
        return Jwts.builder().subject(username).claim("rol", rol).issuedAt(issuedAt)
                .expiration(new Date(issuedAt.getTime() + expirationMillis))
                .signWith(signingKey, Jwts.SIG.HS256).compact();
    }

    public boolean validateToken(String token) {
        try {
            claims(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return claims(token).getSubject();
    }

    private Claims claims(String token) {
        var signed = parser.parseSignedClaims(token);
        if (!"HS256".equals(signed.getHeader().getAlgorithm())) {
            throw new MalformedJwtException("El algoritmo del token no está permitido");
        }
        Claims claims = signed.getPayload();
        String username = claims.getSubject();
        String rol = claims.get("rol", String.class);
        if (username == null || !username.matches("[A-Za-z0-9_.-]{3,50}")
                || claims.getIssuedAt() == null || claims.getExpiration() == null
                || rol == null || !Set.of("ADMIN", "VET", "RECEPCIONISTA").contains(rol)) {
            throw new MalformedJwtException("El token no contiene una identidad válida");
        }
        return claims;
    }
}
