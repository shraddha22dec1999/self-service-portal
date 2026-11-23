package com.mishcon.portal.catalog;

import com.mishcon.portal.common.ApiException;
import com.mishcon.portal.common.EventPublisher;
import com.mishcon.portal.common.OpaClient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/catalogues")
public class CatalogController {
    private final CatalogueRepository repo;
    private final OpaClient opa;
    private final EventPublisher events;

    public CatalogController(CatalogueRepository repo, OpaClient opa, EventPublisher events) {
        this.repo = repo;
        this.opa = opa;
        this.events = events;
    }

    // ---- views -----------------------------------------------------------------

    public record CapabilityView(long id, String name, String risk, String description, List<String> options) {
        static CapabilityView of(Capability c) {
            return new CapabilityView(c.id, c.name, c.risk, c.description, List.copyOf(c.options));
        }
    }

    public record ToolView(String name, String tool, String category, String url, String description) {
        static ToolView of(ToolEntry t) {
            return new ToolView(t.name, t.tool, t.category, t.url, t.description);
        }
    }

    public record CatalogueSummary(String id, String name, String department, String businessUnit, String creator,
                                   String l1, String l2, String description, List<String> visibleTo, int capabilityCount) {
        static CatalogueSummary of(Catalogue c) {
            return new CatalogueSummary(c.id, c.name, c.department, c.businessUnit, c.creator, c.l1, c.l2,
                    c.description, List.copyOf(c.visibleTo), c.capabilities.size());
        }
    }

    public record CatalogueDetail(CatalogueSummary catalogue, List<CapabilityView> capabilities) {}

    public record ServiceDetails(CatalogueSummary catalogue, List<CapabilityView> servicesProvided, List<ToolView> toolsAndResources) {}

    // ---- inputs ----------------------------------------------------------------

    public record CreateCatalogue(@NotBlank String name, @NotBlank String department, @NotBlank String businessUnit,
                                  @NotBlank String l1, @NotBlank String l2, @NotBlank String description) {}

    public record CreateCapability(@NotBlank String name, @NotBlank String risk, @NotBlank String description,
                                   @NotEmpty List<@NotBlank String> options) {}

    public record ToolInput(@NotBlank String name, @NotBlank String tool, @NotBlank String category,
                            @NotBlank String url, String description) {}

    // ---- endpoints -------------------------------------------------------------

    @GetMapping
    @Transactional(readOnly = true)
    public List<CatalogueSummary> list(@RequestHeader("X-User") String user) {
        return repo.findAll().stream().filter(c -> canView(user, c.id)).map(CatalogueSummary::of).toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public CatalogueDetail get(@PathVariable String id, @RequestHeader("X-User") String user) {
        Catalogue c = find(id);
        requireView(user, id);
        return new CatalogueDetail(CatalogueSummary.of(c), c.capabilities.stream().map(CapabilityView::of).toList());
    }

    @GetMapping("/{id}/capabilities/{capId}")
    @Transactional(readOnly = true)
    public CapabilityView capability(@PathVariable String id, @PathVariable long capId, @RequestHeader("X-User") String user) {
        Catalogue c = find(id);
        requireView(user, id);
        return c.capabilities.stream().filter(x -> x.id == capId).findFirst().map(CapabilityView::of)
                .orElseThrow(() -> ApiException.notFound("Capability"));
    }

    @GetMapping("/{id}/service-details")
    @Transactional(readOnly = true)
    public ServiceDetails serviceDetails(@PathVariable String id, @RequestHeader("X-User") String user) {
        Catalogue c = find(id);
        requireView(user, id);
        return new ServiceDetails(CatalogueSummary.of(c),
                c.capabilities.stream().map(CapabilityView::of).toList(),
                c.tools.stream().map(ToolView::of).toList());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public CatalogueSummary create(@Valid @RequestBody CreateCatalogue in, @RequestHeader("X-User") String user) {
        opa.require(Map.of("action", "request", "user", user, "backup_owner", in.l1()));
        String id = in.name().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "") + "_catalogue";
        if (repo.existsById(id)) {
            throw ApiException.conflict("CATALOGUE_EXISTS", "A catalogue with this name already exists");
        }
        // A new catalogue is visible to its creator, approvers and super admins only.
        // Widen it with an OPA configuration request.
        Catalogue c = repo.save(new Catalogue(id, in.name(), in.department(), in.businessUnit(), user,
                in.l1(), in.l2(), in.description(), List.of()));
        opa.putData("catalogs/" + id, Map.of("creator", user, "visible_to", List.of()));
        events.audit(user, "catalogue.created", null, "Created catalogue " + c.name);
        return CatalogueSummary.of(c);
    }

    @PostMapping("/{id}/capabilities")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public CapabilityView addCapability(@PathVariable String id, @Valid @RequestBody CreateCapability in,
                                        @RequestHeader("X-User") String user) {
        Catalogue c = find(id);
        opa.require(Map.of("action", "add_access", "user", user, "catalog", id));
        if (!List.of("Low", "Medium", "High").contains(in.risk())) {
            throw ApiException.badRequest("BAD_RISK", "Risk must be Low, Medium or High");
        }
        Capability cap = new Capability(c, in.name(), in.risk(), in.description(), in.options());
        c.capabilities.add(cap);
        repo.saveAndFlush(c);
        events.audit(user, "capability.added", null, "Added " + in.name() + " to " + c.name);
        return CapabilityView.of(c.capabilities.get(c.capabilities.size() - 1));
    }

    @PutMapping("/{id}/tools")
    @Transactional
    public List<ToolView> replaceTools(@PathVariable String id, @Valid @RequestBody List<@Valid ToolInput> in,
                                       @RequestHeader("X-User") String user) {
        Catalogue c = find(id);
        opa.require(Map.of("action", "add_access", "user", user, "catalog", id));
        c.tools.clear();
        in.forEach(t -> c.tools.add(new ToolEntry(c, t.name(), t.tool(), t.category(), t.url(), t.description())));
        repo.saveAndFlush(c);
        events.audit(user, "tools.updated", null, "Updated tools and resources of " + c.name);
        return c.tools.stream().map(ToolView::of).toList();
    }

    /** Internal: used by the request service to read approvers and capability details. */
    @GetMapping("/internal/{id}")
    @Transactional(readOnly = true)
    public CatalogueDetail internal(@PathVariable String id) {
        Catalogue c = find(id);
        return new CatalogueDetail(CatalogueSummary.of(c), c.capabilities.stream().map(CapabilityView::of).toList());
    }

    // ---- helpers ---------------------------------------------------------------

    private Catalogue find(String id) {
        return repo.findById(id).orElseThrow(() -> ApiException.notFound("Catalogue"));
    }

    private boolean canView(String user, String id) {
        return opa.decide(Map.of("action", "view_catalog", "user", user, "catalog", id)).allow();
    }

    private void requireView(String user, String id) {
        if (!canView(user, id)) {
            throw ApiException.forbidden("You cannot see this catalogue");
        }
    }
}
