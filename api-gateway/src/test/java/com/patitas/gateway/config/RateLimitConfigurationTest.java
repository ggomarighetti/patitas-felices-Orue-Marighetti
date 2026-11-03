package com.patitas.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;
import java.net.InetSocketAddress;

class RateLimitConfigurationTest {

    private final KeyResolver resolver = new RateLimitConfiguration().clientIpKeyResolver();

    @Test
    void remoteAddressDefinesTheQuotaWithoutTrustingUserHeadersOrQuery() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .get("/api/turnos?user=otra-clave")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 45000))
                .header("X-Forwarded-For", "203.0.113.99"));

        StepVerifier.create(resolver.resolve(exchange)).expectNext("127.0.0.1").verifyComplete();
    }

    @Test
    void missingRemoteAddressDoesNotInventAnUnlimitedKey() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/turnos"));

        StepVerifier.create(resolver.resolve(exchange)).verifyComplete();
    }
}
