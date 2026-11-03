package com.patitas.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import java.net.InetSocketAddress;

@Configuration
public class RateLimitConfiguration {

    @Bean
    public KeyResolver clientIpKeyResolver() {
        return exchange -> {
            InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
            return remoteAddress == null ? Mono.empty() : Mono.just(remoteAddress.getHostString());
        };
    }
}
