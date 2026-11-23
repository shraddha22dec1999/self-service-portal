package com.mishcon.portal.provisioning;

import com.mishcon.portal.common.ApiException;
import com.mishcon.portal.common.EventPublisher;
import com.mishcon.portal.common.OpaClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProvisioningService {
    private static final Logger log = LoggerFactory.getLogger(ProvisioningService.class);

    private final JobRepository jobs;
    private final List<Connector> connectors;
    private final OpaClient opa;
    private final EventPublisher events;
    private final ObjectMapper json;

    public ProvisioningService(JobRepository jobs, List<Connector> connectors, OpaClient opa, EventPublisher events,
                               ObjectMapper json) {
        this.jobs = jobs;
        this.connectors = connectors;
        this.opa = opa;
        this.events = events;
        this.json = json;
    }

    /** Picks the first connector that supports the capability. The generic one is last and takes anything. */
    Connector connectorFor(String capability) {
        return connectors.stream().filter(c -> c.supports(capability)).findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "NO_CONNECTOR", "No connector for " + capability));
    }

    static Instant expiry(GrantRequest r, Instant from) {
        if (r.permanent()) return null;
        return from.atZone(ZoneOffset.UTC).plusMonths(r.months()).plusDays(r.days()).toInstant();
    }

    @Transactional
    public Job grant(GrantRequest r) {
        Job j = new Job();
        j.requestId = r.requestId();
        j.capability = r.capability();
        j.optionName = r.option();
        j.requester = r.requester();
        try {
            j.requestJson = json.writeValueAsString(r);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        try {
            if (r.opaPath() != null && !r.opaPath().isBlank()) {
                opa.putData(r.opaPath(), r.opaValue());
                j.connector = "opa";
                j.message = "Published " + r.opaPath() + " to OPA";
            } else {
                Connector c = connectorFor(r.capability());
                j.connector = c.name();
                j.message = c.grant(r);
                j.expiresAt = expiry(r, Instant.now());
            }
            j.status = Job.Status.SUCCEEDED;
        } catch (RuntimeException e) {
            log.warn("Provisioning failed for {}: {}", r.requestId(), e.getMessage());
            j.status = Job.Status.FAILED;
            j.message = e.getMessage();
        }
        Job saved = jobs.save(j);
        events.audit("provisioner", "access." + (saved.status == Job.Status.SUCCEEDED ? "granted" : "failed"),
                r.requestId(), saved.message);
        events.notify(r.requester(), "Access " + (saved.status == Job.Status.SUCCEEDED ? "granted" : "failed"),
                r.requestId() + ": " + saved.message);
        return saved;
    }

    @Transactional
    public Job retry(long jobId) {
        Job old = jobs.findById(jobId).orElseThrow(() -> ApiException.notFound("Job"));
        if (old.status != Job.Status.FAILED) {
            throw ApiException.conflict("NOT_FAILED", "Only a failed job can be retried");
        }
        try {
            return grant(json.readValue(old.requestJson, GrantRequest.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    @Transactional
    public Job revoke(String requestId) {
        Job j = jobs.findByRequestIdOrderByIdDesc(requestId).stream()
                .filter(x -> x.status == Job.Status.SUCCEEDED).findFirst()
                .orElseThrow(() -> ApiException.notFound("Active grant for " + requestId));
        try {
            GrantRequest r = json.readValue(j.requestJson, GrantRequest.class);
            j.message = connectorFor(r.capability()).revoke(r);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        j.status = Job.Status.REVOKED;
        events.audit("provisioner", "access.revoked", requestId, j.message);
        events.notify(j.requester, "Access removed", requestId + ": " + j.message);
        return jobs.save(j);
    }

    /** Every hour: take away access whose end date has passed. */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void revokeExpired() {
        for (Job j : jobs.findByStatusAndExpiresAtBefore(Job.Status.SUCCEEDED, Instant.now().truncatedTo(ChronoUnit.SECONDS))) {
            log.info("Expiring {}", j.requestId);
            revoke(j.requestId);
        }
    }

    public List<Job> forRequest(String requestId) {
        return jobs.findByRequestIdOrderByIdDesc(requestId);
    }
}
