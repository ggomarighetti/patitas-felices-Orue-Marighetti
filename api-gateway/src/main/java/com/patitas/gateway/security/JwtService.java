package com.patitas.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Service
public class JwtService {

    private final JwtParser parser;

    public JwtService(@Value("${jwt.secret}") String secret) {
        parser = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).build();
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
