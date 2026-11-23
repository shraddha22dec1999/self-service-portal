package com.mishcon.portal.audit;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
    private final AuditRepository repo;

    public AuditService(AuditRepository repo) {
        this.repo = repo;
    }

    /** Appends one entry. Synchronized so two writers cannot both extend the same previous hash. */
    @Transactional
    public synchronized AuditEntry append(String actor, String action, String requestId, String message) {
        AuditEntry last = repo.findTopByOrderBySeqDesc();
        String prev = last == null ? HashChain.GENESIS : last.hash;
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        String rid = requestId == null ? "" : requestId;
        String hash = HashChain.hash(prev, now, actor, action, rid, message);
        return repo.save(new AuditEntry(now, actor, action, rid, message, prev, hash));
    }

    public List<AuditEntry> search(String actor, String requestId, Instant from, Instant to) {
        return repo.search(actor, requestId,
                from == null ? Instant.EPOCH : from,
                to == null ? Instant.now().plus(1, ChronoUnit.DAYS) : to);
    }

    /** Walks the whole chain. Returns the sequence number of the first broken entry, or null when intact. */
    public Long firstBroken() {
        String prev = HashChain.GENESIS;
        for (AuditEntry e : repo.findAllByOrderBySeqAsc()) {
            String expected = HashChain.hash(prev, e.time, e.actor, e.action, e.requestId, e.message);
            if (!prev.equals(e.prevHash) || !expected.equals(e.hash)) {
                return e.seq;
            }
            prev = e.hash;
        }
        return null;
    }

    public Map<String, Long> countByAction() {
        Map<String, Long> out = new LinkedHashMap<>();
        for (AuditEntry e : repo.findAllByOrderBySeqAsc()) {
            out.merge(e.action, 1L, Long::sum);
        }
        return out;
    }
}
