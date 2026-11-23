package com.mishcon.portal.gateway;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
class SecurityConfig {

    /** Production: a valid SSO (OpenID Connect) token is required on every call except health. */
    @Bean
    @ConditionalOnProperty(name = "portal.auth.enabled", havingValue = "true")
    SecurityWebFilterChain secured(ServerHttpSecurity http) {
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(a -> a.pathMatchers("/actuator/health/**").permitAll().anyExchange().authenticated())
                .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()))
                .build();
    }

    /** Local development only: no token check. */
    @Bean
    @ConditionalOnProperty(name = "portal.auth.enabled", havingValue = "false", matchIfMissing = true)
    SecurityWebFilterChain open(ServerHttpSecurity http) {
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(a -> a.anyExchange().permitAll())
                .build();
    }
}
