package com.patitas.gateway.filters;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.net.URI;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class LoggingFilterTest {

    private final LoggingFilter filter = new LoggingFilter();
    private Logger logger;
    private Level previousLevel;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(LoggingFilter.class);
        previousLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
        logger.setLevel(previousLevel);
    }

    @Test
    void missingExplicitStatusIsLoggedAsDefaultSuccess() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/duenos"));
        GatewayFilterChain chain = current -> current.getResponse().setComplete();

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertEquals(2, messages().size());
        assertTrue(messages().get(0).contains("--> GET /api/duenos"));
        assertTrue(messages().get(1).matches(".*<-- GET /api/duenos 200 \\d+ms.*"));
    }

    @Test
    void webAndGatewayChainsProduceOnlyOnePairOfLogs() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/turnos"));
        GatewayFilterChain gatewayChain = current -> {
            current.getResponse().setStatusCode(HttpStatus.CREATED);
            return current.getResponse().setComplete();
        };
        WebFilterChain webChain = current -> filter.filter(current, gatewayChain);

        StepVerifier.create(filter.filter(exchange, webChain)).verifyComplete();

        assertEquals(2, messages().size());
        assertTrue(messages().get(1).contains("POST /api/turnos 201"));
    }

    @Test
    void errorHandlerCanLogTheFinalStatusWithoutLosingTheOriginalError() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/mascotas"));
        RuntimeException original = new IllegalStateException("Destino no disponible");
        GatewayFilterChain chain = current -> Mono.error(original);

        StepVerifier.create(filter.filter(exchange, chain).onErrorResume(error -> {
            assertSame(original, error);
            exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            return exchange.getResponse().setComplete();
        })).verifyComplete();

        assertEquals(2, messages().size());
        assertTrue(messages().get(1).contains("GET /api/mascotas 503"));
    }

    @Test
    void headersAndQueryValuesAreNotIncludedInTheLog() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .get("/api/duenos?token=query-secret").header("Authorization", "Bearer header-secret"));
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR,
                URI.create("http://service:8081/api/duenos?token=query-secret"));
        GatewayFilterChain chain = current -> current.getResponse().setComplete();

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertTrue(messages().get(1).contains("upstream=service:8081"));
        assertTrue(messages().stream().noneMatch(message -> message.contains("secret") || message.contains("token=")));
    }

    private List<String> messages() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }
}
