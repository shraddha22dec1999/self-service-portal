# Self Service Portal: microservices

Backend for the self service access portal: catalogues, requests, two-level approval,
automatic provisioning, a tamper-evident audit log and notifications. Open Policy Agent (OPA)
decides who may do what, so rules live in policy files and data, not in Java.

Java 21, Spring Boot 3.3, PostgreSQL 16, OPA 0.70. Maven multi-module.

## Services

| Service | Port | What it does |
| --- | --- | --- |
| `gateway` | 8080 | Only entry point. Checks the SSO token, sets `X-User`, routes to the services, blocks `/internal` |
| `catalog-service` | 8081 | Catalogues, capability cards and options, Service details (services provided, tools and resources) |
| `request-service` | 8082 | Requests, approval chain (L1, L2), delegation, cancel, escalation, hands approved requests to provisioning |
| `provisioning-service` | 8083 | Gives and removes access through connectors, expiry job, publishes approved OPA changes |
| `audit-service` | 8084 | Append-only hash-chained log, verify, CSV export, counts |
| `notification-service` | 8085 | Stores notifications per person |
| `opa` | 8181 | Policies in `opa/` (`approval.rego`, `hierarchy.rego`, `data.json`) |

`portal-common` holds what every service shares: error format, OPA client, audit and notification sender.

```
Browser ──> gateway ──> catalog / request / provisioning / audit / notification
                              │            │
                              └── OPA ◄────┘   request ──> provisioning ──> connector
                                               every step ──> audit + notification
```

## Run it

You need Docker.

```
docker compose up --build
```

Then open `requests.http` in VS Code (REST Client) or IntelliJ and send the calls in order.
In local mode there is no SSO. You say who you are with the `X-User` header. The sample people
(`app_engg_member_1`, `sol_engg_lead`, `manpreet`, `amit`, `super_admin_1`) come from `opa/data.json`.

Without Docker: start OPA (`opa run --server opa/`), then run each service with
`mvn -pl services/<name> -am spring-boot:run`. Without the `docker` profile each service uses an in-memory H2 database.

## The main flow

1. `POST /api/v1/requests` as `app_engg_member_1`. OPA checks the backup owner. OPA also says who L1 and L2 are for that person.
2. `GET /api/v1/requests?scope=approvals` as `sol_engg_lead` shows it. `POST /{id}/decision` approves it.
3. After L2 approves, the request service calls provisioning. The connector runs, the request becomes `ACTIVE`, and audit and notification record it.
4. `GET /api/v1/audit/verify` walks the hash chain.

## APIs

All paths are under `/api/v1` on the gateway.

| Area | Calls |
| --- | --- |
| Me | `GET /me` |
| Catalogue | `GET /catalogues`, `GET /catalogues/{id}`, `GET /catalogues/{id}/capabilities/{capId}`, `GET /catalogues/{id}/service-details`, `POST /catalogues`, `POST /catalogues/{id}/capabilities`, `PUT /catalogues/{id}/tools` |
| Requests | `POST /requests`, `GET /requests?scope=mine\|approvals\|decided`, `GET /requests/{id}`, `GET /requests/{id}/activity`, `POST /requests/{id}/decision`, `POST /requests/{id}/delegate`, `POST /requests/{id}/cancel` |
| Provisioning | `GET /provisioning/jobs?requestId=`, `POST /provisioning/jobs/{id}/retry`, `POST /provisioning/revocations` |
| Audit | `GET /audit`, `GET /audit/verify`, `GET /audit/export`, `GET /analytics/summary` |
| Notifications | `GET /notifications`, `POST /notifications/{id}/read` |

Errors always look like `{ "code": "...", "message": "..." }`. A policy refusal is `403` with the reasons from OPA.
If OPA cannot be reached the answer is `503`. The services never fall back to "allow".

## OPA

`opa/data.json` holds teams, approvers, super admins and which teams see which catalogue.
`opa/approval.rego` holds the rules. Run the tests with `opa test opa -v` and `opa check --strict opa`.

An approved OPA change is carried by a request with `opaPath` and `opaValue`. When the last
approval comes in, provisioning calls `PUT /v1/data/<opaPath>` on OPA. See the last example in `requests.http`.
This changes OPA's memory. To keep it after a restart, also commit the change to `opa/data.json`
(or serve OPA from a bundle server and have provisioning push there).

## Turn on SSO

Set `SPRING_PROFILES_ACTIVE=sso`, `OIDC_ISSUER_URI=<your identity provider issuer>` and `AUTH_ENABLED=true` on the gateway.
Every call then needs a valid bearer token, and `X-User` is taken from the `preferred_username` claim
(`AUTH_USER_CLAIM` changes the claim). Anything the browser sends as `X-User` is thrown away.
The other services trust the gateway, so do not publish their ports. Keep them on the private network.

## What is a stub or missing

Read this before going live.

- **Connectors only write a log line.** `provisioning-service/.../Connectors.java`. Put the real call to GitHub, Microsoft Graph, Atlassian or your cloud IAM in `grant` and `revoke`.
- **REST between services, not Kafka.** `EventPublisher` is the one place to change when you add Kafka.
- **Notifications are stored, not emailed.** Add a sender in `NotificationController`.
- **Database schema is created by Hibernate** (`ddl-auto: update`). Switch to Flyway before production.
- **Billing and cost, and team and role listing for Service details** are not built. Only services provided and tools and resources are.
- **`/me` returns the user id only.** Add role and menus from OPA when the UI needs them.
- **Seed data** in `catalog-service/.../SeedData.java` is sample data. Delete it when you load real catalogues.
- **Written without being compiled or run.** The code was written in an environment with no access to Maven Central, so it has not been built, and the tests have not been run. First step after import: `mvn verify`. Expect to fix a few small things.

## Import into GitHub

```
cd self-service-portal
git init -b main
git add .
git commit -m "Initial commit: self service portal microservices"
gh repo create <your-org>/self-service-portal --private --source . --push
```

Or create an empty repository on github.com and run the `git remote add origin ...` and `git push -u origin main` lines it shows.
The workflow in `.github/workflows/ci.yml` then builds, tests the services and the policies, and builds the images.
