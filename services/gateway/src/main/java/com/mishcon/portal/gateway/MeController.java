package com.mishcon.portal.gateway;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
class MeController {
    private final boolean authEnabled;
    private final String claim;

    MeController(@Value("${portal.auth.enabled:false}") boolean authEnabled,
                 @Value("${portal.auth.user-claim:preferred_username}") String claim) {
        this.authEnabled = authEnabled;
        this.claim = claim;
    }

    /**
     * Who the services will see. With SSO on this comes from the token only. Extend this to return
     * the role and menus from OPA.
     */
    @GetMapping("/api/v1/me")
    Mono<Map<String, String>> me(@RequestHeader(value = "X-User", defaultValue = "") String header) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .filter(a -> a instanceof JwtAuthenticationToken)
                .map(a -> ((JwtAuthenticationToken) a).getToken().getClaimAsString(claim))
                .switchIfEmpty(Mono.fromSupplier(() -> authEnabled ? "" : header))
                .map(user -> Map.of("id", user));
    }
}
