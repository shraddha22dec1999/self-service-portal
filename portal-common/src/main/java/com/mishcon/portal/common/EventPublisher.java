package com.mishcon.portal.common;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Sends audit entries and notifications to their services over REST. Best effort: a failure here
 * is logged and never breaks the user's request. Swap this class for a Kafka producer when you
 * want asynchronous delivery. The callers do not change.
 */
@Component
public class EventPublisher {
    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);
    private final RestClient audit;
    private final RestClient notify;

    public EventPublisher(@Value("${portal.audit.url:http://localhost:8084}") String auditUrl,
                          @Value("${portal.notification.url:http://localhost:8085}") String notifyUrl) {
        this.audit = RestClient.builder().baseUrl(auditUrl).build();
        this.notify = RestClient.builder().baseUrl(notifyUrl).build();
    }

    public void audit(String actor, String action, String requestId, String message) {
        try {
            audit.post().uri("/internal/audit")
                    .body(Map.of("actor", actor, "action", action,
                            "requestId", requestId == null ? "" : requestId, "message", message))
                    .retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.warn("Could not write audit entry {}: {}", action, e.getMessage());
        }
    }

    public void notify(String to, String subject, String body) {
        try {
            notify.post().uri("/internal/notifications")
                    .body(Map.of("recipient", to, "subject", subject, "body", body))
                    .retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.warn("Could not send notification to {}: {}", to, e.getMessage());
        }
    }
}
