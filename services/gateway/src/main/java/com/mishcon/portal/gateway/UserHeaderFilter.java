package com.mishcon.portal.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Two jobs:
 * 1. Never forward anything under /internal. Those endpoints are for service to service calls only.
 * 2. Tell the services who is calling. With SSO on, X-User comes from the token and any X-User sent
 *    by the browser is thrown away. With SSO off (local development) the caller's own X-User is used.
 */
@Component
public class UserHeaderFilter implements GlobalFilter, Ordered {
    private final boolean authEnabled;
    private final String claim;

    public UserHeaderFilter(@Value("${portal.auth.enabled:false}") boolean authEnabled,
                            @Value("${portal.auth.user-claim:preferred_username}") String claim) {
        this.authEnabled = authEnabled;
        this.claim = claim;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (exchange.getRequest().getURI().getPath().contains("/internal")) {
            exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
            return exchange.getResponse().setComplete();
        }
        if (!authEnabled) {
            return chain.filter(exchange);
        }
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .filter(a -> a instanceof JwtAuthenticationToken)
                .map(a -> ((JwtAuthenticationToken) a).getToken().getClaimAsString(claim))
                .defaultIfEmpty("")
                .flatMap(user -> {
                    if (user.isBlank()) {
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }
                    ServerHttpRequest request = exchange.getRequest().mutate()
                            .headers(h -> {
                                h.remove("X-User");
                                h.add("X-User", user);
                            }).build();
                    return chain.filter(exchange.mutate().request(request).build());
                });
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
