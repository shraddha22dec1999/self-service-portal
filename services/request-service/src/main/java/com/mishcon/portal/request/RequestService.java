package com.mishcon.portal.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mishcon.portal.common.ApiException;
import com.mishcon.portal.common.EventPublisher;
import com.mishcon.portal.common.OpaClient;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

/** The request life cycle: create, approve or reject, delegate, cancel, then hand over to provisioning. */
@Service
public class RequestService {
    private static final Logger log = LoggerFactory.getLogger(RequestService.class);

    private final RequestRepository repo;
    private final OpaClient opa;
    private final EventPublisher events;
    private final Clients clients;
    private final ObjectMapper json;
    private final Set<String> superAdmins;

    public RequestService(RequestRepository repo, OpaClient opa, EventPublisher events, Clients clients,
                          ObjectMapper json, @Value("${portal.super-admins:}") String superAdmins) {
        this.repo = repo;
        this.opa = opa;
        this.events = events;
        this.clients = clients;
        this.json = json;
        this.superAdmins = new LinkedHashSet<>(Arrays.stream(superAdmins.split(",")).map(String::trim)
                .filter(s -> !s.isEmpty()).toList());
    }

    // ---- create ----------------------------------------------------------------

    @Transactional
    public AccessRequest create(RequestController.CreateRequest in, String user) {
        opa.require(Map.of("action", "request", "user", user, "backup_owner", in.backupOwner()));

        Clients.CatalogueDetail cat = loadCatalogue(in.catalogueId());
        Clients.CapabilityView cap = cat.capabilities().stream().filter(c -> c.name().equals(in.capability()))
                .findFirst().orElseThrow(() -> ApiException.badRequest("BAD_CAPABILITY", "Unknown capability " + in.capability()));
        if (!cap.options().contains(in.option())) {
            throw ApiException.badRequest("BAD_OPTION", "Unknown option " + in.option());
        }
        if (!in.permanent() && in.months() == 0 && in.days() == 0) {
            throw ApiException.badRequest("BAD_DURATION", "Choose at least 1 day or 1 month, or permanent access");
        }

        AccessRequest r = new AccessRequest(in.catalogueId(), in.capability(), in.option(), user, in.backupOwner(),
                in.justification(), in.permanent(), in.months(), in.days());
        if (in.opaPath() != null && !in.opaPath().isBlank()) {
            r.opaPath = in.opaPath();
            try {
                r.opaValueJson = json.writeValueAsString(in.opaValue());
            } catch (JsonProcessingException e) {
                throw ApiException.badRequest("BAD_OPA_VALUE", "opaValue is not valid JSON");
            }
        }

        List<String> l1 = approversFor(user, "L1", cat.catalogue().l1());
        List<String> l2 = approversFor(user, "L2", cat.catalogue().l2());
        if (new LinkedHashSet<>(l1).equals(new LinkedHashSet<>(l2))) {
            r.steps.add(new ApprovalStep(r, 0, "L1+L2", String.join(",", l1)));
        } else {
            r.steps.add(new ApprovalStep(r, 0, "L1", String.join(",", l1)));
            r.steps.add(new ApprovalStep(r, 1, "L2", String.join(",", l2)));
        }
        r = repo.saveAndFlush(r);

        events.audit(user, "request.created", r.id(), "Requested " + r.capability + ": " + r.optionName
                + ", backup owner " + r.backupOwner);
        notifyCurrent(r, "Approval needed");
        return r;
    }

    private List<String> approversFor(String requester, String level, String catalogueFallback) {
        List<String> a = opa.decide(Map.of("action", "approvers", "requester", requester, "step", level, "user", requester)).approvers();
        return a.isEmpty() ? List.of(catalogueFallback) : a;
    }

    private Clients.CatalogueDetail loadCatalogue(String id) {
        try {
            return clients.catalogue(id);
        } catch (HttpClientErrorException.NotFound e) {
            throw ApiException.notFound("Catalogue " + id);
        } catch (RestClientException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "CATALOG_UNAVAILABLE", "Catalogue service is not reachable");
        }
    }

    // ---- decide, delegate, cancel -----------------------------------------------

    @Transactional
    public AccessRequest decide(String id, String user, ApprovalStep.Decision decision, String comment) {
        AccessRequest r = pending(id);
        ApprovalStep step = r.current();
        if (decision == ApprovalStep.Decision.REJECTED && (comment == null || comment.isBlank())) {
            throw ApiException.badRequest("COMMENT_REQUIRED", "A comment is required when rejecting");
        }
        Map<String, Object> input = new HashMap<>();
        input.put("action", decision == ApprovalStep.Decision.APPROVED ? "approve" : "reject");
        input.put("user", user);
        input.put("requester", r.requester);
        input.put("step", step.level.startsWith("L1") ? "L1" : "L2");
        input.put("delegates", step.delegatedFrom == null ? List.of() : csv(step.assignees));
        opa.require(input);

        step.decision = decision;
        step.decidedBy = user;
        step.decidedAt = Instant.now();
        step.comment = comment;
        events.audit(user, "request." + decision.name().toLowerCase(), r.id(),
                step.level + " " + decision.name().toLowerCase() + (comment == null || comment.isBlank() ? "" : ": " + comment));

        if (decision == ApprovalStep.Decision.REJECTED) {
            r.status = AccessRequest.Status.REJECTED;
            r.decidedAt = Instant.now();
            events.notify(r.requester, "Request rejected", r.id() + " was rejected by " + user + ". " + (comment == null ? "" : comment));
        } else if (r.current() == null) {
            r.decidedAt = Instant.now();
            provision(r);
        } else {
            notifyCurrent(r, "Approval needed");
        }
        return repo.save(r);
    }

    @Transactional
    public AccessRequest delegate(String id, String user, String to) {
        AccessRequest r = pending(id);
        ApprovalStep step = r.current();
        Map<String, Object> input = new HashMap<>();
        input.put("action", "delegate");
        input.put("user", user);
        input.put("requester", r.requester);
        input.put("step", step.level.startsWith("L1") ? "L1" : "L2");
        input.put("delegate_to", to);
        opa.require(input);

        step.delegatedFrom = user;
        step.assignees = to;
        events.audit(user, "request.delegated", r.id(), step.level + " delegated to " + to);
        events.notify(to, "Approval delegated to you", user + " delegated " + r.id() + " to you.");
        return repo.save(r);
    }

    @Transactional
    public AccessRequest cancel(String id, String user) {
        AccessRequest r = pending(id);
        if (!r.requester.equals(user)) {
            throw ApiException.forbidden("Only the requester can cancel a request");
        }
        r.status = AccessRequest.Status.CANCELLED;
        r.decidedAt = Instant.now();
        events.audit(user, "request.cancelled", r.id(), "Cancelled by the requester");
        return repo.save(r);
    }

    private void provision(AccessRequest r) {
        r.status = AccessRequest.Status.PROVISIONING;
        Map<String, Object> grant = new HashMap<>();
        grant.put("requestId", r.id());
        grant.put("catalogueId", r.catalogueId);
        grant.put("capability", r.capability);
        grant.put("option", r.optionName);
        grant.put("requester", r.requester);
        grant.put("permanent", r.permanent);
        grant.put("months", r.months);
        grant.put("days", r.days);
        if (r.opaPath != null) {
            grant.put("opaPath", r.opaPath);
            try {
                grant.put("opaValue", json.readValue(r.opaValueJson, Map.class));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException(e);
            }
        }
        try {
            Clients.JobView job = clients.provision(grant);
            r.status = "SUCCEEDED".equals(job.status()) ? AccessRequest.Status.ACTIVE : AccessRequest.Status.FAILED;
        } catch (RestClientException e) {
            log.warn("Provisioning call failed for {}: {}", r.id(), e.getMessage());
            r.status = AccessRequest.Status.FAILED;
            events.audit("system", "provisioning.unreachable", r.id(), "Provisioning service could not be reached");
        }
    }

    // ---- reads -------------------------------------------------------------------

    @Transactional(readOnly = true)
    public AccessRequest get(String id, String user) {
        AccessRequest r = find(id);
        List<String> approvers = new ArrayList<>();
        r.steps.forEach(s -> {
            approvers.addAll(csv(s.assignees));
            if (s.decidedBy != null) approvers.add(s.decidedBy);
        });
        opa.require(Map.of("action", "view", "user", user,
                "request", Map.of("requester", r.requester, "backup_owner", r.backupOwner, "approvers", approvers)));
        return r;
    }

    @Transactional(readOnly = true)
    public List<AccessRequest> list(String scope, String user) {
        return switch (scope) {
            case "mine" -> repo.findByRequesterOrderBySeqDesc(user);
            case "approvals" -> repo.findByStatusOrderBySeqDesc(AccessRequest.Status.PENDING_APPROVAL).stream()
                    .filter(r -> !r.requester.equals(user))
                    .filter(r -> superAdmins.contains(user) || csv(r.current().assignees).contains(user)).toList();
            case "decided" -> repo.findAllByOrderBySeqDesc().stream()
                    .filter(r -> r.steps.stream().anyMatch(s -> user.equals(s.decidedBy))).toList();
            default -> throw ApiException.badRequest("BAD_SCOPE", "scope must be mine, approvals or decided");
        };
    }

    public List<Map<String, Object>> activity(String id, String user) {
        get(id, user);
        return clients.activity(find(id).id());
    }

    // ---- escalation ---------------------------------------------------------------

    /**
     * Every 5 minutes: ask OPA again who should approve each waiting step. If an approver was
     * marked inactive in the meantime, the step moves up (L1 to L2, then to super admins).
     * Delegated steps are left alone.
     */
    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    @Transactional
    public void reroute() {
        for (AccessRequest r : repo.findByStatusOrderBySeqDesc(AccessRequest.Status.PENDING_APPROVAL)) {
            ApprovalStep s = r.current();
            if (s == null || s.delegatedFrom != null) continue;
            List<String> now = opa.decide(Map.of("action", "approvers", "user", r.requester, "requester", r.requester,
                    "step", s.level.startsWith("L1") ? "L1" : "L2")).approvers();
            if (!now.isEmpty() && !new LinkedHashSet<>(now).equals(new LinkedHashSet<>(csv(s.assignees)))) {
                String before = s.assignees;
                s.assignees = String.join(",", now);
                events.audit("system", "request.escalated", r.id(), s.level + " moved from " + before + " to " + s.assignees);
                notifyCurrent(r, "Approval needed (escalated)");
            }
        }
    }

    // ---- helpers -------------------------------------------------------------------

    private AccessRequest find(String id) {
        return repo.findById(AccessRequest.seqOf(id)).orElseThrow(() -> ApiException.notFound("Request " + id));
    }

    private AccessRequest pending(String id) {
        AccessRequest r = find(id);
        if (r.status != AccessRequest.Status.PENDING_APPROVAL || r.current() == null) {
            throw ApiException.conflict("NOT_PENDING", r.id() + " is " + r.status.name().toLowerCase().replace('_', ' '));
        }
        return r;
    }

    private void notifyCurrent(AccessRequest r, String subject) {
        ApprovalStep s = r.current();
        if (s != null) {
            csv(s.assignees).forEach(a -> events.notify(a, subject, r.id() + ": " + r.capability + " (" + r.optionName + ") for " + r.requester));
        }
    }

    static List<String> csv(String s) {
        return s == null || s.isBlank() ? List.of() : Arrays.stream(s.split(",")).map(String::trim).toList();
    }
}
