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

## Live resources and current verification

- Render workspace: AlgoBloom; service `algobloom-demo` (`srv-db8nhojl550s73ehgvt0`), plan `free`,
  region Oregon, branch `codex/v2-core-mvp`, automatic deploys disabled.
- Neon project: `algobloom-demo` (`spring-salad-62135871`), verified Free organization, PostgreSQL 17,
  region aws-us-west-2, minimal compute. It contains no copied production records.
- Public frontend/API origin: https://algobloom-demo.onrender.com
- Database-backed health: https://algobloom-demo.onrender.com/health — HTTP 200.
- `/coach`, `/journal`, `/growth` and favicon return HTTP 200; anonymous bootstrap returns 401.
- Flyway V1–V5 applied successfully to the newly created empty database. TLS uses Java’s default CA
  trust store with `verify-full`, retaining certificate and hostname verification.
- Local regression: 137 backend tests and 12 frontend tests passed; TypeScript/build passed. The
  Linux Docker image built successfully; cloud startup exposed and resolved endpoint-label/TLS
  configuration differences before health verification.
- Public real-AI smoke passed nine checks on October 15, 2026: HTTPS registration/login, real AI
  draft generation without a Journal, idempotent confirmation, canonical Workspace links, real L1
  tutoring without completion, concept discussion without hint escalation, shared feedback/Growth,
  conversation/Journal recovery after logout/login, and another plan without another Journal.
- The public browser also showed the thinking state and a real streamed stack/queue explanation;
  authenticated Dashboard layout had no horizontal overflow at 360 px. Original synthetic messages
  were inspected for meaning; this is sampled verification, not independent AI-quality certification.
- Run `python3 scripts/coach_public_smoke.py --real` for the bounded fixed-demo-only smoke; it incurs
  provider usage and retains a synthetic report under ignored `target/`. It never reads an API key.
- Initial provider checks correctly rejected a rule/application fallback before the saved secret
  was loaded. The new demo was reloaded and only genuine provider replies counted as a pass.
- Deployed application commit: `bfaca3932e6650d64580f520e0dc10ad0b39c202`. Subsequent documentation
  and smoke-script commits do not change the deployed application. Auto-deploy remains disabled.
- The demo has a 10-message/user/day setting. This is an application quota, not a provider-wide
  monetary limit. Monitor provider usage; do not enable automatic credit replenishment for a demo.

No paid plan, payment method, production database operation or Railway setting change was introduced.
