package com.mishcon.portal.notification;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

/**
 * Stores each notification so the portal can show it. To also send email or chat messages, add a
 * sender here (for example JavaMailSender or a Slack webhook) next to the log line.
 */
@RestController
public class NotificationController {
    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);
    private final NotificationRepository repo;

    public NotificationController(NotificationRepository repo) {
        this.repo = repo;
    }

    public record SendRequest(@NotBlank String recipient, @NotBlank String subject, @NotBlank String body) {}

    public record View(long id, String subject, String body, Instant createdAt, boolean read) {}

    /** Called by the other services. Not exposed through the gateway. */
    @PostMapping("/internal/notifications")
    public View send(@Valid @RequestBody SendRequest r) {
        Notification n = repo.save(new Notification(r.recipient(), r.subject(), r.body()));
        log.info("Notification for {}: {}", r.recipient(), r.subject());
        return view(n);
    }

    @GetMapping("/api/v1/notifications")
    public List<View> mine(@RequestHeader("X-User") String user) {
        return repo.findByRecipientOrderByIdDesc(user).stream().map(NotificationController::view).toList();
    }

    @PostMapping("/api/v1/notifications/{id}/read")
    public View markRead(@PathVariable long id, @RequestHeader("X-User") String user) {
        Notification n = repo.findById(id).filter(x -> x.recipient.equals(user))
                .orElseThrow(() -> com.mishcon.portal.common.ApiException.notFound("Notification"));
        n.read = true;
        return view(repo.save(n));
    }

    private static View view(Notification n) {
        return new View(n.id, n.subject, n.body, n.createdAt, n.read);
    }
}
