package com.mishcon.portal.provisioning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ProvisioningTest {
    private static GrantRequest req(boolean permanent, int months, int days) {
        return new GrantRequest("REQ-1001", "iam_catalogue", "Grant repository access", "Read access",
                "app_engg_member_1", permanent, months, days, null, null);
    }

    @Test
    void permanentAccessNeverExpires() {
        assertNull(ProvisioningService.expiry(req(true, 0, 0), Instant.parse("2026-11-23T00:00:00Z")));
    }

    @Test
    void monthsAndDaysAreAddedToTheStart() {
        Instant end = ProvisioningService.expiry(req(false, 2, 10), Instant.parse("2026-11-23T00:00:00Z"));
        assertEquals(Instant.parse("2027-02-02T00:00:00Z"), end);
    }
}
