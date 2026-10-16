# Actual repository audit — Product Spec 1.1

Inspected `/Users/marilyn/Desktop/leetcode-review-system`, clean `main` at `b71e92e` before changes.
Development branch: `codex/v2-core-mvp`. No replacement project was created. No AGENTS.md was found
in the repository/inspected parent chain. Existing uncommitted user edits were absent.

- **Existing and reused:** Java 21 / Spring Boot 4.1.1 (`pom.xml`); JPA/PostgreSQL; `Problem`, `Pattern`,
  `ReviewSession`; legacy CRUD/scheduling/statistics (`ProblemService.java`); M1 metadata-only catalog
  (`CatalogService.java`); explicit local test datasource and 64 baseline tests.
- **Partial and extended:** M1 separates canonical patterns but retains review state on Problem. New
  attempts are independent; legacy state is neither moved nor invented as session history. Existing
  CRUD implementation remains, with explicit local compatibility access for unowned personal data.
- **Missing at baseline:** frontend, identity/ownership, persistent chat, planning/confirmation,
  session/Journal/attempt feedback, personalized review, memory, positive growth, LLM provider,
  incremental migration chain. Added under `coach/`, `frontend/` and `db/migration/`.
- **Conflict with new Spec:** Hibernate `ddl-auto=update` and test `create-drop` were present. Replaced
  with validation and additive local Flyway migrations; no destructive schema operation is run.
- **Copyright boundary:** the old M1 TSV/attribution is retained unchanged. Coach's default path does
  not consume NeetCode ordering; `CoachCatalogInitializer.java` explicitly defines its own small order,
  original conceptual teaching contexts and per-entry provenance. No proprietary statements/solutions.
- **Deployment boundary:** existing Railway datasource variable names are preserved, but no Railway
  connection/deployment was made. Local Coach profile has a fixed loopback pool, also selected by Flyway.

Technical choices: modular monolith; Spring Security cookie sessions/BCrypt/CSRF; additive Flyway;
React/TypeScript + Vite; Java HttpClient OpenAI Responses adapter rather than an extra LLM framework.
Official function-calling documentation was consulted; external network/tool execution is not exposed
to the model. Real-provider verification remains blocked on a development key.

See `CORE_MVP.md` for run/security semantics and `ACCEPTANCE_1_1.md` for release status. This audit is
repository evidence, not a claim of production readiness or a completed real-AI MVP.
