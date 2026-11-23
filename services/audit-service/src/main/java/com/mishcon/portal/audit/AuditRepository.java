package com.mishcon.portal.audit;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditRepository extends JpaRepository<AuditEntry, Long> {
    AuditEntry findTopByOrderBySeqDesc();

    List<AuditEntry> findAllByOrderBySeqAsc();

    @Query("""
            select e from AuditEntry e
            where (:actor is null or e.actor = :actor)
              and (:requestId is null or e.requestId = :requestId)
              and e.time >= :from and e.time <= :to
            order by e.seq desc
            """)
    List<AuditEntry> search(@Param("actor") String actor, @Param("requestId") String requestId,
                            @Param("from") Instant from, @Param("to") Instant to);
}
