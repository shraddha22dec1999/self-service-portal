package com.mishcon.portal.request;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "access_requests")
public class AccessRequest {
    public enum Status { PENDING_APPROVAL, PROVISIONING, ACTIVE, REJECTED, CANCELLED, FAILED }

    /** The database number. The visible id is REQ-(1000 + seq). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long seq;
    String catalogueId;
    String capability;
    String optionName;
    String requester;
    String backupOwner;
    @Column(length = 2000)
    String justification;
    boolean permanent;
    int months;
    int days;
    @Enumerated(EnumType.STRING)
    Status status = Status.PENDING_APPROVAL;
    Instant createdAt = Instant.now();
    Instant decidedAt;
    /** Set when this request is an OPA configuration change. */
    String opaPath;
    @Column(length = 4000)
    String opaValueJson;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("position ASC")
    List<ApprovalStep> steps = new ArrayList<>();

    protected AccessRequest() {}

    AccessRequest(String catalogueId, String capability, String optionName, String requester, String backupOwner,
                  String justification, boolean permanent, int months, int days) {
        this.catalogueId = catalogueId;
        this.capability = capability;
        this.optionName = optionName;
        this.requester = requester;
        this.backupOwner = backupOwner;
        this.justification = justification;
        this.permanent = permanent;
        this.months = months;
        this.days = days;
    }

    String id() {
        return "REQ-" + (1000 + seq);
    }

    static long seqOf(String id) {
        try {
            return Long.parseLong(id.replaceFirst("^REQ-", "")) - 1000;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** First step without a decision, or null when all are decided. */
    ApprovalStep current() {
        return steps.stream().filter(s -> s.decision == null).findFirst().orElse(null);
    }
}
