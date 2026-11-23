package com.mishcon.portal.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class AccessRequestTest {
    @Test
    void idRoundTrips() {
        AccessRequest r = new AccessRequest("c", "cap", "opt", "u", "b", "j", true, 0, 0);
        r.seq = 45L;
        assertEquals("REQ-1045", r.id());
        assertEquals(45L, AccessRequest.seqOf("REQ-1045"));
        assertEquals(-1L, AccessRequest.seqOf("nonsense"));
    }

    @Test
    void currentIsTheFirstUndecidedStep() {
        AccessRequest r = new AccessRequest("c", "cap", "opt", "u", "b", "j", true, 0, 0);
        ApprovalStep s1 = new ApprovalStep(r, 0, "L1", "a");
        ApprovalStep s2 = new ApprovalStep(r, 1, "L2", "b");
        r.steps.add(s1);
        r.steps.add(s2);
        assertEquals(s1, r.current());
        s1.decision = ApprovalStep.Decision.APPROVED;
        assertEquals(s2, r.current());
        s2.decision = ApprovalStep.Decision.APPROVED;
        assertNull(r.current());
    }

    @Test
    void csvSplitsAndTrims() {
        assertEquals(2, RequestService.csv("a, b").size());
        assertEquals(0, RequestService.csv("").size());
    }
}
