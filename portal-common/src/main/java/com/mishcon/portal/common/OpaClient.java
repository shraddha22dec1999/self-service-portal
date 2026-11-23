package com.mishcon.portal.common;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Asks Open Policy Agent what is allowed. Fails closed: if OPA cannot be reached the
 * caller gets 503, never an "allow".
 */
@Component
public class OpaClient {
    private final RestClient http;

    public OpaClient(@Value("${portal.opa.url:http://localhost:8181}") String url) {
        this.http = RestClient.builder().baseUrl(url).build();
    }

    public record Decision(boolean allow, List<String> reasons, List<String> approvers) {
        public String reasonText() {
            return reasons.isEmpty() ? "Not allowed by policy" : String.join("; ", reasons);
        }
    }

    /** POST /v1/data/portal/approval/decision */
    @SuppressWarnings("unchecked")
    public Decision decide(Map<String, Object> input) {
        try {
            Map<String, Object> body = http.post().uri("/v1/data/portal/approval/decision")
                    .body(Map.of("input", input)).retrieve().body(Map.class);
            Map<String, Object> r = body == null ? null : (Map<String, Object>) body.get("result");
            if (r == null) {
                return new Decision(false, List.of("policy returned no result"), List.of());
            }
            return new Decision(Boolean.TRUE.equals(r.get("allow")),
                    (List<String>) r.getOrDefault("reasons", List.of()),
                    (List<String>) r.getOrDefault("approvers", List.of()));
        } catch (RestClientException e) {
            throw unavailable(e);
        }
    }

    /** Throws 403 with the reasons when the decision is a no. */
    public Decision require(Map<String, Object> input) {
        Decision d = decide(input);
        if (!d.allow()) {
            throw ApiException.forbidden(d.reasonText());
        }
        return d;
    }

    /** PUT /v1/data/{path}. Used to publish an approved OPA configuration change. */
    public void putData(String path, Object value) {
        try {
            http.put().uri("/v1/data/" + path.replaceFirst("^/", "")).body(value).retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            throw unavailable(e);
        }
    }

    private ApiException unavailable(Exception e) {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "OPA_UNAVAILABLE", "Policy engine is not reachable");
    }
}
