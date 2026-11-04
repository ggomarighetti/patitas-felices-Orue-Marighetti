package com.patitas.gateway.filter;

import com.patitas.gateway.security.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.test.StepVerifier;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticationFilterTest {

    private static final String TEST_SECRET = "test-only-gateway-key-0123456789abcdef0123456789abcdef0123456789abcdef";
    private final JwtService jwt = spy(new JwtService(TEST_SECRET));
    private final AuthenticationFilter filter = new AuthenticationFilter(jwt);

    @ParameterizedTest
    @ValueSource(strings = {"/api/duenos", "/api/mascotas", "/api/veterinarios", "/api/turnos",
            "/ms-dueno/api/duenos", "/ms-mascota/api/mascotas", "/ms-veterinario/api/veterinarios",
            "/ms-turno/api/turnos", "/v1/duenos", "/actuator/health", "/actuator/gateway/routes",
            "/desconocida", "/authenticacion", "/auth/../api/mascotas"})
    void everyProtectedEntryIsRejectedBeforeTheDownstreamChain(String path) {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path));
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        StepVerifier.create(filter.filter(exchange, downstream(forwarded))).verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        assertNull(forwarded.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Basic abc", "Bearer ", "Bearer falso", "Bearer a.b.c"})
    void incorrectAuthorizationIsRejected(String authorization) {
        MockServerWebExchange exchange = MockServerWebExchange.from(requestWithAuthorization(authorization));
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        StepVerifier.create(filter.filter(exchange, downstream(forwarded))).verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        assertNull(forwarded.get());
    }

    @Test
    void validIdentityReplacesSpoofedHeadersAndDoesNotForwardTheBearerToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/mascotas")
                .header("Authorization", "Bearer " + token(new Date(System.currentTimeMillis() + 60000), TEST_SECRET))
                .header("X-Auth-User", "falso", "otro").header("X-Auth-Rol", "ADMIN"));
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        StepVerifier.create(filter.filter(exchange, downstream(forwarded))).verifyComplete();

        var headers = forwarded.get().getRequest().getHeaders();
        assertEquals(java.util.List.of("jorge"), headers.get("X-Auth-User"));
        assertNull(headers.getFirst("Authorization"));
        assertNull(headers.getFirst("X-Auth-Rol"));
    }

    @Test
    void loginAndRegisterRemainPublicAndRemoveForgedIdentity() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/auth/login")
                .header("X-Auth-User", "falso"));
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        StepVerifier.create(filter.filter(exchange, downstream(forwarded))).verifyComplete();

        assertNotNull(forwarded.get());
        assertNull(forwarded.get().getRequest().getHeaders().getFirst("X-Auth-User"));
        verifyNoInteractions(jwt);
    }

    @Test
    void duplicateAuthorizationHeadersAreRejected() {
        String token = token(new Date(System.currentTimeMillis() + 60000), TEST_SECRET);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/mascotas")
                .header("Authorization", "Bearer " + token, "Bearer " + token));
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        StepVerifier.create(filter.filter(exchange, downstream(forwarded))).verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        assertNull(forwarded.get());
    }

    @Test
    void expiredAndForeignTokensNeverReachTheBackend() {
        for (String token : java.util.List.of(token(new Date(1000), TEST_SECRET),
                token(new Date(System.currentTimeMillis() + 60000), TEST_SECRET + "foreign"))) {
            MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/mascotas")
                    .header("Authorization", "Bearer " + token));
            AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

            StepVerifier.create(filter.filter(exchange, downstream(forwarded))).verifyComplete();

            assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
            assertNull(forwarded.get());
        }
    }

    @Test
    void webAndGatewayChainsValidateOnlyOnce() {
        String token = token(new Date(System.currentTimeMillis() + 60000), TEST_SECRET);
        MockServerWebExchange exchange = MockServerWebExchange.from(requestWithAuthorization("Bearer " + token));
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        WebFilterChain webChain = current -> filter.filter(current, downstream(forwarded));

        StepVerifier.create(filter.filter(exchange, webChain)).verifyComplete();

        assertEquals("jorge", forwarded.get().getRequest().getHeaders().getFirst("X-Auth-User"));
        verify(jwt).validateToken(token);
        verify(jwt).extractUsername(token);
    }

    @Test
    void properCorsPreflightIsAllowedWithoutInventingAnIdentity() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.options("/api/mascotas")
                .header("Origin", "http://localhost:5500").header("Access-Control-Request-Method", "GET"));
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        StepVerifier.create(filter.filter(exchange, downstream(forwarded))).verifyComplete();

        assertNotNull(forwarded.get());
        assertNull(forwarded.get().getRequest().getHeaders().getFirst("X-Auth-User"));
    }

    private MockServerHttpRequest.BaseBuilder<?> requestWithAuthorization(String authorization) {
        return MockServerHttpRequest.get("/api/mascotas").header("Authorization", authorization);
    }

    private GatewayFilterChain downstream(AtomicReference<ServerWebExchange> forwarded) {
        return current -> {
            forwarded.set(current);
            return current.getResponse().setComplete();
        };
    }

    private String token(Date expiration, String secret) {
        return Jwts.builder().subject("jorge").claim("rol", "ADMIN").issuedAt(new Date(0))
                .expiration(expiration).signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256).compact();
    }
}
