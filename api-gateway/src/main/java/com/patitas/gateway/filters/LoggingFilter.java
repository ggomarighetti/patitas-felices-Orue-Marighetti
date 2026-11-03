package com.patitas.gateway.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import java.net.URI;
import java.util.concurrent.TimeUnit;

@Component
public class LoggingFilter implements GlobalFilter, WebFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);
    private static final String LOGGING_ATTRIBUTE = LoggingFilter.class.getName();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        prepareLogging(exchange);
        return chain.filter(exchange);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        prepareLogging(exchange);
        return chain.filter(exchange);
    }

    private void prepareLogging(ServerWebExchange exchange) {
        if (exchange.getAttributes().putIfAbsent(LOGGING_ATTRIBUTE, Boolean.TRUE) != null) {
            return;
        }
        long start = System.nanoTime();
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getPath().value();
        log.info("[GATEWAY] --> {} {}", method, path);
        exchange.getResponse().beforeCommit(() -> {
            long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            HttpStatusCode status = exchange.getResponse().getStatusCode();
            Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            URI upstream = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
            log.info("[GATEWAY] <-- {} {} {} {}ms route={} upstream={}", method, path,
                    status == null ? 200 : status.value(), duration,
                    route == null ? "-" : route.getId(), upstream == null ? "-" : upstream.getAuthority());
            return Mono.empty();
        });
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
