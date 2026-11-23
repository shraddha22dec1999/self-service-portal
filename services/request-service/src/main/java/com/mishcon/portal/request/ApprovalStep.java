package com.mishcon.portal.request;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "approval_steps")
public class ApprovalStep {
    public enum Decision { APPROVED, REJECTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    int position;
    /** L1 or L2. */
    String level;
    /** Comma separated. Any one of them can decide. OPA decides who they are, including escalation. */
    String assignees;
    String delegatedFrom;
    @Enumerated(EnumType.STRING)
    Decision decision;
    String decidedBy;
    Instant decidedAt;
    @Column(length = 1000)
    String comment;

    @ManyToOne(optional = false)
    @JoinColumn(name = "request_seq")
    AccessRequest request;

    protected ApprovalStep() {}

    ApprovalStep(AccessRequest request, int position, String level, String assignees) {
        this.request = request;
        this.position = position;
        this.level = level;
        this.assignees = assignees;
    }
}
