package com.mishcon.portal.provisioning;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/** What the request service sends once the last approval is in. */
public record GrantRequest(
        @NotBlank String requestId,
        @NotBlank String catalogueId,
        @NotBlank String capability,
        @NotBlank String option,
        @NotBlank String requester,
        boolean permanent,
        @Min(0) @Max(24) int months,
        @Min(0) @Max(30) int days,
        /** Optional. For an approved OPA change: the data path, for example catalogs/iam_catalogue/visible_to. */
        String opaPath,
        /** Optional. The new value for opaPath. */
        Map<String, Object> opaValue) {
}
