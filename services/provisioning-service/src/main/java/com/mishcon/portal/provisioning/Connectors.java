package com.mishcon.portal.provisioning;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * The connectors that ship with the project. They only write a log line. Replace the body of
 * grant and revoke with a call to the real system (GitHub REST, Microsoft Graph, Atlassian
 * Cloud API, cloud IAM) and keep the rest.
 */
@Configuration
class Connectors {
    private static final Logger log = LoggerFactory.getLogger(Connectors.class);

    private static Connector simulated(String name, String... capabilityWords) {
        return new Connector() {
            public String name() { return name; }

            public boolean supports(String capability) {
                String c = capability.toLowerCase();
                for (String w : capabilityWords) {
                    if (c.contains(w)) return true;
                }
                return false;
            }

            public String grant(GrantRequest r) {
                log.info("[{}] GRANT {} / {} to {}", name, r.capability(), r.option(), r.requester());
                return "Granted " + r.option() + " to " + r.requester() + " (" + name + ")";
            }

            public String revoke(GrantRequest r) {
                log.info("[{}] REVOKE {} / {} from {}", name, r.capability(), r.option(), r.requester());
                return "Revoked " + r.option() + " from " + r.requester() + " (" + name + ")";
            }
        };
    }

    @Bean @Order(1)
    Connector github() { return simulated("github", "repository", "github"); }

    @Bean @Order(2)
    Connector atlassian() { return simulated("atlassian", "jira", "confluence"); }

    @Bean @Order(3)
    Connector directory() { return simulated("directory", "ad group", "joiner", "member"); }

    @Bean @Order(4)
    Connector cloud() { return simulated("cloud", "cloud", "environment", "resource group"); }

    /** Takes anything the others do not. Keep it last. */
    @Bean @Order(Ordered.LOWEST_PRECEDENCE)
    Connector generic() { return simulated("generic", ""); }
}
