# AlgoBloom free demo deployment

Target: one **Free** Render web service (Java backend + bundled React SPA), and a separate
**Free** Neon PostgreSQL project. This is a portfolio demo, not a paid always-on production SLA.
No Railway production service, connection or database is changed. No payment method or upgrade is
required by this plan; if an account demands one, stop and inspect rather than accepting charges.

Render free services can sleep after 15 idle minutes and take about a minute to wake. Monthly
compute/build/egress quotas apply. Use a free workspace without a payment method so quota exhaustion
suspends service rather than generating overage charges. Neon free usage also has quotas. OpenAI
usage is separate: real Coach requests consume provider credit even when hosting is free.

## Deployment design

The multi-stage Dockerfile builds React using the committed lockfile, copies its output into the
Spring Boot jar, and runs Java 21 as a non-root user with a bounded JVM heap. Render needs Docker
for Java; no separate frontend server or cross-origin cookie configuration is needed.

Use branch `codex/v2-core-mvp`, the root Dockerfile and the Free service plan. Keep automatic deploys
off while configuring and validating the demo. `/health` checks the database; known client routes
forward to `index.html`. Unknown API routes never return the SPA. Default/local profiles are unchanged.

## Backend-only service settings

- `SPRING_PROFILES_ACTIVE=staging`
- `ALGOBLOOM_STAGING_SERVICE_ID`: the newly created Render service ID, matching `RENDER_SERVICE_ID`.
- `ALGOBLOOM_STAGING_DB_HOST`: the endpoint of the **new** Neon project.
- `ALGOBLOOM_STAGING_JDBC_URL`: `jdbc:postgresql://<new-neon-host>:5432/neondb?sslmode=verify-full`
- `ALGOBLOOM_STAGING_DB_USER`, `ALGOBLOOM_STAGING_DB_PASSWORD`: credentials for that new database.
- `COACH_OPENAI_API_KEY`: configure in Render's secret environment-variable UI, never in Git/frontend.
- `COACH_OPENAI_MODEL`: default `gpt-4.1-mini`.
- `PORT`: Render supplies the listening port. Secure session cookies require the public HTTPS URL.

The explicit staging pool validates Render identity and the pinned Neon endpoint before opening any
connection. Flyway baseline is disabled and runs only on the new empty database; Hibernate validates.
The reviewed metadata/teaching-path seed runs after migrations. Never copy production connection
strings, users, records or backups into this environment. Existing test pools remain fixed loopback.

## Verification gates

Local: backend tests, frontend tests, TypeScript/build, diff/secret checks. Public: HTTPS asset/SPA
routes, database health, registration/login, planning and confirmation, Workspace, shared feedback,
Journal persistence and Growth; real AI smoke only after configuring the provider secret. Report
cold-start/restart behavior and limitations honestly. A config file or successful build is not proof
of a live deployment. Do not label the launch complete until the actual public URL passes these checks.

Current status: local preparation only; Render and Neon authentication/provisioning and a real
public endpoint remain pending. No new hosting resources have been created.
