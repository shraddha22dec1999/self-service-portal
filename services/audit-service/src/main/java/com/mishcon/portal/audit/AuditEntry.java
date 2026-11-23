package com.mishcon.portal.audit;

import jakarta.persistence.*;
import java.time.Instant;

/** One line of the audit log. Never updated or deleted. Each line holds the hash of the line before it. */
@Entity
@Table(name = "audit_entries")
public class AuditEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long seq;
    Instant time;
    String actor;
    String action;
    String requestId;
    @Column(length = 2000)
    String message;
    String prevHash;
    String hash;

    protected AuditEntry() {}

    AuditEntry(Instant time, String actor, String action, String requestId, String message, String prevHash, String hash) {
        this.time = time;
        this.actor = actor;
        this.action = action;
        this.requestId = requestId;
        this.message = message;
        this.prevHash = prevHash;
        this.hash = hash;
    }
}
