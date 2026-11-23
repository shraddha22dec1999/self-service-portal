package com.mishcon.portal.provisioning;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByRequestIdOrderByIdDesc(String requestId);

    List<Job> findByStatusAndExpiresAtBefore(Job.Status status, Instant time);
}
