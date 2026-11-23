package com.mishcon.portal.request;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** REST calls to the catalog and provisioning services. */
@Component
class Clients {
    private final RestClient catalog;
    private final RestClient provisioning;
    private final RestClient audit;

    Clients(@Value("${portal.catalog.url}") String catalogUrl,
            @Value("${portal.provisioning.url}") String provisioningUrl,
            @Value("${portal.audit.url}") String auditUrl) {
        this.catalog = RestClient.builder().baseUrl(catalogUrl).build();
        this.provisioning = RestClient.builder().baseUrl(provisioningUrl).build();
        this.audit = RestClient.builder().baseUrl(auditUrl).build();
    }

    record CatalogueView(String id, String name, String l1, String l2) {}

    record CapabilityView(String name, String risk, List<String> options) {}

    record CatalogueDetail(CatalogueView catalogue, List<CapabilityView> capabilities) {}

    record JobView(long id, String status, String message) {}

    CatalogueDetail catalogue(String id) {
        return catalog.get().uri("/api/v1/catalogues/internal/{id}", id).retrieve().body(CatalogueDetail.class);
    }

    JobView provision(Map<String, Object> grant) {
        return provisioning.post().uri("/internal/provisioning/jobs").body(grant).retrieve().body(JobView.class);
    }

    List<Map<String, Object>> activity(String requestId) {
        return audit.get().uri("/api/v1/audit?requestId={r}", requestId).retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<Map<String, Object>>>() {});
    }
}
