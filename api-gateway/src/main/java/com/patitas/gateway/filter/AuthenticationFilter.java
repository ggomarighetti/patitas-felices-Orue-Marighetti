package com.patitas.gateway.filter;

import com.patitas.gateway.security.JwtService;
import io.jsonwebtoken.JwtException;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.function.Function;

@Component
public class AuthenticationFilter implements GlobalFilter, WebFilter, Ordered {

    private static final String AUTHENTICATED_ATTRIBUTE = AuthenticationFilter.class.getName();
    private final JwtService jwtService;

    public AuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return authenticate(exchange, chain::filter);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return authenticate(exchange, chain::filter);
    }

    private Mono<Void> authenticate(ServerWebExchange exchange, Function<ServerWebExchange, Mono<Void>> chain) {
        if (exchange.getAttribute(AUTHENTICATED_ATTRIBUTE) != null) {
            return chain.apply(exchange);
        }
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        boolean publicAuth = path.startsWith("/auth/") && !path.contains("..")
                && !path.contains("%") && !path.contains("\\");
        if (publicAuth || CorsUtils.isPreFlightRequest(request)) {
            exchange.getAttributes().put(AUTHENTICATED_ATTRIBUTE, Boolean.TRUE);
            return chain.apply(exchange.mutate().request(sanitized(request, null)).build());
        }
        String username = authenticatedUsername(request);
        if (username == null) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            exchange.getResponse().getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            return exchange.getResponse().setComplete();
        }
        exchange.getAttributes().put(AUTHENTICATED_ATTRIBUTE, Boolean.TRUE);
        return chain.apply(exchange.mutate().request(sanitized(request, username)).build());
    }

    private String authenticatedUsername(ServerHttpRequest request) {
        List<String> authorization = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (authorization == null || authorization.size() != 1) {
            return null;
        }
        String header = authorization.getFirst();
        if (header.length() <= 7 || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        String token = header.substring(7).trim();
        try {
            return jwtService.validateToken(token) ? jwtService.extractUsername(token) : null;
        } catch (JwtException | IllegalArgumentException exception) {
            return null;
        }
    }

    private ServerHttpRequest sanitized(ServerHttpRequest request, String username) {
        return request.mutate().headers(headers -> {
            headers.remove(HttpHeaders.AUTHORIZATION);
            headers.remove("X-Auth-User");
            headers.remove("X-Auth-Rol");
            if (username != null) {
                headers.set("X-Auth-User", username);
            }
        }).build();
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
