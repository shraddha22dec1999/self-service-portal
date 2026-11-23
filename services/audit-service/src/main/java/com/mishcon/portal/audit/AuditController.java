package com.mishcon.portal.audit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuditController {
    private final AuditService service;

    public AuditController(AuditService service) {
        this.service = service;
    }

    public record AppendRequest(@NotBlank String actor, @NotBlank String action, String requestId, @NotBlank String message) {}

    public record EntryView(long seq, Instant time, String actor, String action, String requestId, String message,
                            String prevHash, String hash) {
        static EntryView of(AuditEntry e) {
            return new EntryView(e.seq, e.time, e.actor, e.action, e.requestId, e.message, e.prevHash, e.hash);
        }
    }

    /** Called by the other services. Not exposed through the gateway. */
    @PostMapping("/internal/audit")
    public EntryView append(@Valid @RequestBody AppendRequest r) {
        return EntryView.of(service.append(r.actor(), r.action(), r.requestId(), r.message()));
    }

    @GetMapping("/api/v1/audit")
    public List<EntryView> search(@RequestParam(required = false) String actor,
                                  @RequestParam(required = false) String requestId,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return service.search(actor, requestId, from, to).stream().map(EntryView::of).toList();
    }

    @GetMapping("/api/v1/audit/verify")
    public Map<String, Object> verify() {
        Long broken = service.firstBroken();
        return broken == null ? Map.of("intact", true) : Map.of("intact", false, "firstBrokenSeq", broken);
    }

    @GetMapping(value = "/api/v1/audit/export", produces = "text/csv")
    public ResponseEntity<String> export() {
        StringBuilder sb = new StringBuilder("seq,time,actor,action,requestId,message,hash\n");
        for (AuditEntry row : service.search(null, null, null, null).reversed()) {
            EntryView e = EntryView.of(row);
            sb.append(e.seq()).append(',').append(e.time()).append(',').append(csv(e.actor())).append(',')
                    .append(csv(e.action())).append(',').append(csv(e.requestId())).append(',')
                    .append(csv(e.message())).append(',').append(e.hash()).append('\n');
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit.csv")
                .contentType(MediaType.parseMediaType("text/csv")).body(sb.toString());
    }

    @GetMapping("/api/v1/analytics/summary")
    public Map<String, Long> summary() {
        return service.countByAction();
    }

    private static String csv(String s) {
        String v = s == null ? "" : s;
        // Stop spreadsheet formula injection, then quote.
        if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        return '"' + v.replace("\"", "\"\"") + '"';
    }
}
