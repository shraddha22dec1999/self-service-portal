package com.mishcon.portal.catalog;

import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Sample data so the service is useful on first start. The people and teams match opa/data.json.
 * Delete this class when you load real catalogues.
 */
@Component
class SeedData implements CommandLineRunner {
    private final CatalogueRepository repo;

    SeedData(CatalogueRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(String... args) {
        if (repo.count() > 0) {
            return;
        }
        Catalogue app = new Catalogue("app_engg_catalogue", "Product", "Technology", "Engineering", "sol_engg_lead",
                "sol_engg_lead", "manpreet",
                "From repo to release. Each card is a capability. Pick what you want to do under it.",
                List.of("App Engg"));
        add(app, "Grant repository access", "Medium", "Work with code in a GitHub repository.",
                "Read access", "Write access", "Admin access", "Reviewer access");
        add(app, "Run GitHub workflow", "Low", "Trigger build and release workflows.",
                "Run a build workflow", "Run a release workflow");
        add(app, "Create a Jira project", "Low", "Set up a project for your team.",
                "Software project", "Service project", "Business project");
        app.tools.add(new ToolEntry(app, "Source code", "GitHub", "Code", "https://github.example.com/mishcon/product", "Application repositories"));
        app.tools.add(new ToolEntry(app, "Work board", "Jira", "Work tracking", "https://jira.example.com/boards/PRD", "Sprint board"));
        app.tools.add(new ToolEntry(app, "Team documentation", "Confluence", "Documentation", "https://confluence.example.com/spaces/PRD", "Design notes"));

        Catalogue cloud = new Catalogue("cloud_catalogue", "Cloud and Infrastructure", "Infrastructure", "Platform",
                "amit", "manpreet", "amit", "Provision environments and manage cloud access.", List.of("App Engg", "Sol Engg"));
        add(cloud, "Provision an environment", "High", "New dev, test or prod environment.",
                "Create a dev environment", "Create a test environment", "Create a prod environment");
        add(cloud, "Request a cloud role", "High", "Time-bound role in a cloud account.",
                "Reader", "Contributor", "Owner (time-bound)");

        Catalogue iam = new Catalogue("iam_catalogue", "Identity and Access", "Security", "Security",
                "amit", "manpreet", "amit", "Directory groups, Jira and Confluence access.", List.of("All teams"));
        add(iam, "Add member to AD group", "Low", "Add or remove group members.",
                "Add a member", "Remove a member", "Add a member for a limited time");
        add(iam, "Jira or Confluence access", "Low", "Project and space access.",
                "Jira project access", "Confluence space access");

        repo.saveAll(List.of(app, cloud, iam));
    }

    private static void add(Catalogue c, String name, String risk, String desc, String... options) {
        c.capabilities.add(new Capability(c, name, risk, desc, List.of(options)));
    }
}
