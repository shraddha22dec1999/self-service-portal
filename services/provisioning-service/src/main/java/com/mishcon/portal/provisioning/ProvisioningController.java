package com.mishcon.portal.provisioning;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
public class ProvisioningController {
    private final ProvisioningService service;

    public ProvisioningController(ProvisioningService service) {
        this.service = service;
    }

    public record JobView(long id, String requestId, String connector, String capability, String option, String status,
                          String message, Instant createdAt, Instant expiresAt) {
        static JobView of(Job j) {
            return new JobView(j.id, j.requestId, j.connector, j.capability, j.optionName, j.status.name(),
                    j.message, j.createdAt, j.expiresAt);
        }
    }

    /** Called by the request service after the last approval. Not exposed through the gateway. */
    @PostMapping("/internal/provisioning/jobs")
    public JobView grant(@Valid @RequestBody GrantRequest r) {
        return JobView.of(service.grant(r));
    }

    @GetMapping("/api/v1/provisioning/jobs")
    public List<JobView> byRequest(@RequestParam String requestId) {
        return service.forRequest(requestId).stream().map(JobView::of).toList();
    }

    @PostMapping("/api/v1/provisioning/jobs/{id}/retry")
    public JobView retry(@PathVariable long id) {
        return JobView.of(service.retry(id));
    }

    @PostMapping("/api/v1/provisioning/revocations")
    public JobView revoke(@RequestBody Map<String, String> body) {
        return JobView.of(service.revoke(body.getOrDefault("requestId", "")));
    }
}
