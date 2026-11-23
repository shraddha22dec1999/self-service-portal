package com.mishcon.portal.provisioning;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "provisioning_jobs")
public class Job {
    public enum Status { SUCCEEDED, FAILED, REVOKED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String requestId;
    String connector;
    String capability;
    String optionName;
    String requester;
    @Enumerated(EnumType.STRING)
    Status status;
    @Column(length = 1000)
    String message;
    Instant createdAt = Instant.now();
    /** Null means permanent access. */
    Instant expiresAt;
    @Column(length = 4000)
    String requestJson;

    protected Job() {}
}
