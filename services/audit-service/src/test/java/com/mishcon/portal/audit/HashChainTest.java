package com.mishcon.portal.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class HashChainTest {
    private static final Instant T = Instant.parse("2026-11-23T10:00:00Z");

    @Test
    void sameInputGivesSameHash() {
        assertEquals(HashChain.hash("p", T, "a", "x", "REQ-1", "m"), HashChain.hash("p", T, "a", "x", "REQ-1", "m"));
    }

    @Test
    void anyChangeBreaksTheHash() {
        String base = HashChain.hash("p", T, "a", "x", "REQ-1", "m");
        assertNotEquals(base, HashChain.hash("q", T, "a", "x", "REQ-1", "m"));
        assertNotEquals(base, HashChain.hash("p", T, "b", "x", "REQ-1", "m"));
        assertNotEquals(base, HashChain.hash("p", T, "a", "x", "REQ-1", "changed"));
    }
}
