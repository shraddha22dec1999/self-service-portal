package com.mishcon.portal.notification;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String recipient;
    String subject;
    @Column(length = 2000)
    String body;
    Instant createdAt = Instant.now();
    boolean read;

    protected Notification() {}

    Notification(String recipient, String subject, String body) {
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
    }
}
