package com.mishcon.portal.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/requests")
public class RequestController {
    private final RequestService service;

    public RequestController(RequestService service) {
        this.service = service;
    }

    // ---- inputs ----------------------------------------------------------------

    public record CreateRequest(
            @NotBlank String catalogueId,
            @NotBlank String capability,
            @NotBlank String option,
            @NotBlank String justification,
            @NotBlank String backupOwner,
            boolean permanent,
            @Min(0) @Max(24) int months,
            @Min(0) @Max(30) int days,
            /** Only for OPA configuration changes. Example: catalogs/iam_catalogue/visible_to */
            String opaPath,
            Map<String, Object> opaValue) {}

    public record DecisionRequest(@NotNull ApprovalStep.Decision decision, String comment) {}

    public record DelegateRequest(@NotBlank String to) {}

    // ---- views -----------------------------------------------------------------

    public record StepView(String level, List<String> assignees, String delegatedFrom, String decision,
                           String decidedBy, Instant decidedAt, String comment, boolean current) {}

    public record RequestView(String id, String catalogueId, String capability, String option, String requester,
                              String backupOwner, String justification, boolean permanent, int months, int days,
                              String status, Instant createdAt, Instant decidedAt, String opaPath, List<StepView> steps) {
        static RequestView of(AccessRequest r) {
            ApprovalStep cur = r.current();
            List<StepView> steps = r.steps.stream().map(s -> new StepView(s.level, RequestService.csv(s.assignees),
                    s.delegatedFrom, s.decision == null ? null : s.decision.name(), s.decidedBy, s.decidedAt, s.comment,
                    s == cur && r.status == AccessRequest.Status.PENDING_APPROVAL)).toList();
            return new RequestView(r.id(), r.catalogueId, r.capability, r.optionName, r.requester, r.backupOwner,
                    r.justification, r.permanent, r.months, r.days, r.status.name(), r.createdAt, r.decidedAt,
                    r.opaPath, steps);
        }
    }

    // ---- endpoints -------------------------------------------------------------

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RequestView create(@Valid @RequestBody CreateRequest in, @RequestHeader("X-User") String user) {
        return RequestView.of(service.create(in, user));
    }

    /** scope = mine | approvals | decided */
    @GetMapping
    public List<RequestView> list(@RequestParam(defaultValue = "mine") String scope, @RequestHeader("X-User") String user) {
        return service.list(scope, user).stream().map(RequestView::of).toList();
    }

    @GetMapping("/{id}")
    public RequestView get(@PathVariable String id, @RequestHeader("X-User") String user) {
        return RequestView.of(service.get(id, user));
    }

    @GetMapping("/{id}/activity")
    public List<Map<String, Object>> activity(@PathVariable String id, @RequestHeader("X-User") String user) {
        return service.activity(id, user);
    }

    @PostMapping("/{id}/decision")
    public RequestView decide(@PathVariable String id, @Valid @RequestBody DecisionRequest in, @RequestHeader("X-User") String user) {
        return RequestView.of(service.decide(id, user, in.decision(), in.comment()));
    }

    @PostMapping("/{id}/delegate")
    public RequestView delegate(@PathVariable String id, @Valid @RequestBody DelegateRequest in, @RequestHeader("X-User") String user) {
        return RequestView.of(service.delegate(id, user, in.to()));
    }

    @PostMapping("/{id}/cancel")
    public RequestView cancel(@PathVariable String id, @RequestHeader("X-User") String user) {
        return RequestView.of(service.cancel(id, user));
    }
}
