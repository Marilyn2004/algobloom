# Core MVP implementation delivery — Product Spec 1.1

## Status and scope
The local learning loop is implemented and exercised with a real provider. AC01–AC27 have scoped evidence in [ACCEPTANCE_1_1.md](ACCEPTANCE_1_1.md); independent user product/AI-quality acceptance remains pending. This is not a production-readiness certification.

## Reuse and implementation
Existing Spring Boot/PostgreSQL, canonical Problem/Pattern catalog, V1 CRUD/review scheduling and regression tests were retained. New owner-scoped coach persistence and thin REST controllers provide account/session authentication, continuous conversation, controlled planning tools, revisioned drafts, explicit idempotent confirmation, sessions/items/attempts, shared Workspace/Journal feedback, derived review/skill evidence, editable memory, recaps, basic interview planning and growth timeline. React/TypeScript supplies Dashboard, Workspace, Journal, Growth and preferences views. SSE carries request status and the final result, not token-by-token model output.

Real OpenAI Responses integration includes strict tools, bounded context/calls, current-request reveal permission and a semantic tutoring review. Its judgments remain probabilistic. Synthetic multi-problem transcripts and adversarial follow-ups were inspected; they do not establish universal no-spoiler guarantees.

## Running and acceptance
Follow [CORE_MVP.md](CORE_MVP.md) for Java/PostgreSQL/frontend prerequisites and startup. Configure COACH_OPENAI_API_KEY only in the backend terminal, then run `bash scripts/run-local-coach.sh`. Keep demo false for real AI acceptance. The local frontend is http://127.0.0.1:5173/coach. No secrets are committed.

Use chat to request a short plan, adjust the persisted draft, explicitly confirm it, open an item Workspace, follow the official link, request small hints/concepts or paste code, then record feedback and Journal text. Ask for the next plan and browse Growth. Original notes are preserved; corrections update derived evidence. Journal and feedback retain newer input during saves.

## Verification
- Full `./mvnw test` with Java 21: 133 tests, 0 failures, 0 errors, 0 skipped.
- Frontend tests: 12 passed; TypeScript and production build passed.
- Real provider: planning, three-problem hint ladders, concepts, explicit reveal, feedback and next plan; additional free tutoring/debugging and injection probes.
- Browser after final backend restart: thinking status followed by real stack/queue explanation; input cleared and send state recovered.
- Real socket tests cover stream disconnect, persistence and replay without duplicate provider calls.
- Local API synthetic ownership, correction, memory, lifecycle, export and disposable-account deletion checks passed. Sample local read/write p95 3.48/20.52 ms is not a production benchmark.
- Mobile/desktop layouts, keyboard growth navigation, unsaved recovery and version conflicts were sampled. No independent accessibility certification.

## Data compatibility and safety
Flyway V1–V5 are additive, preserve legacy rows and do not fabricate old sessions. Hibernate validates the new local profile; it does not destructively update schema. Literal loopback development/test pools prevent external datasource overrides from redirecting these environments. No production database was accessed, seeded, migrated or deployed. Legacy unowned private endpoints are denied by default; the documented local compatibility option retains old APIs for regression use. A production rollout/migration review remains separate.

## Remaining acceptance and later work
User acceptance of teaching quality and product flow remains pending. Model output may be verbose or incorrect despite review; the correction mechanism records issues without rewriting originals. Production deployment, operational monitoring, durable distributed sessions, backup/recovery and independent security/accessibility review have not been certified. Automatic weekly reports, mock interviews, guaranteed company-specific questions and account synchronization remain Later features.

No commit, push or deployment was performed. Original V2_SPEC.md is unchanged.

## Exact changed-file inventory

### Modified
- `.gitignore`
- `README.md`
- `pom.xml`
- `src/main/resources/application.properties`
- `src/test/java/com/ziyi/leetcodereviewsystem/LocalTestDatabaseConfiguration.java`
- `src/test/java/com/ziyi/leetcodereviewsystem/ProblemControllerTest.java`
- `src/test/java/com/ziyi/leetcodereviewsystem/StatsControllerTest.java`
- `src/test/java/com/ziyi/leetcodereviewsystem/TestDatasourceIsolationTest.java`
- `src/test/resources/application.properties`

### Added
- `.env.example`
- `docs/ACCEPTANCE_1_1.md`
- `docs/CORE_MVP.md`
- `docs/PRODUCT_SPEC_1_1.md`
- `docs/REPOSITORY_AUDIT_1_1.md`
- `frontend/.gitignore`
- `frontend/index.html`
- `frontend/package.json`
- `frontend/pnpm-lock.yaml`
- `frontend/src/api.ts`
- `frontend/src/domain.mjs`
- `frontend/src/main.tsx`
- `frontend/src/style.css`
- `frontend/tests/api.test.mjs`
- `frontend/tests/domain.test.mjs`
- `frontend/tsconfig.json`
- `frontend/vite.config.ts`
- `scripts/coach_catalog_injection_smoke.py`
- `scripts/coach_local_checks.py`
- `scripts/coach_real_smoke.py`
- `scripts/coach_tutoring_followups.py`
- `scripts/run-local-coach.sh`
- `src/main/java/com/ziyi/leetcodereviewsystem/coach/CoachAuthController.java`
- `src/main/java/com/ziyi/leetcodereviewsystem/coach/CoachCatalogInitializer.java`
- `src/main/java/com/ziyi/leetcodereviewsystem/coach/CoachChatService.java`
- `src/main/java/com/ziyi/leetcodereviewsystem/coach/CoachClock.java`
- `src/main/java/com/ziyi/leetcodereviewsystem/coach/CoachController.java`
- `src/main/java/com/ziyi/leetcodereviewsystem/coach/CoachErrors.java`
- `src/main/java/com/ziyi/leetcodereviewsystem/coach/CoachLearningService.java`
- `src/main/java/com/ziyi/leetcodereviewsystem/coach/CoachLocalConfiguration.java`
- `src/main/java/com/ziyi/leetcodereviewsystem/coach/CoachProvider.java`
- `src/main/java/com/ziyi/leetcodereviewsystem/coach/CoachRequestLimit.java`
- `src/main/java/com/ziyi/leetcodereviewsystem/coach/CoachSecurity.java`
- `src/main/java/com/ziyi/leetcodereviewsystem/coach/CoachStore.java`
- `src/main/java/com/ziyi/leetcodereviewsystem/coach/CoachStreamController.java`
- `src/main/java/com/ziyi/leetcodereviewsystem/coach/CoachTutorReview.java`
- `src/main/java/com/ziyi/leetcodereviewsystem/coach/OpenAiCoachProvider.java`
- `src/main/java/com/ziyi/leetcodereviewsystem/coach/TutorPolicy.java`
- `src/main/resources/application-coach.properties`
- `src/main/resources/db/migration/V1__preserve_legacy_schema.sql`
- `src/main/resources/db/migration/V2__coach_learning_loop.sql`
- `src/main/resources/db/migration/V3__persist_cancellations.sql`
- `src/main/resources/db/migration/V4__tutoring_corrections.sql`
- `src/main/resources/db/migration/V5__message_recovery.sql`
- `src/test/java/com/ziyi/leetcodereviewsystem/coach/CoachAcceptanceIntegrationTest.java`
- `src/test/java/com/ziyi/leetcodereviewsystem/coach/CoachConcurrencyTest.java`
- `src/test/java/com/ziyi/leetcodereviewsystem/coach/CoachFlowIntegrationTest.java`
- `src/test/java/com/ziyi/leetcodereviewsystem/coach/CoachMigrationTest.java`
- `src/test/java/com/ziyi/leetcodereviewsystem/coach/CoachProviderTest.java`
- `src/test/java/com/ziyi/leetcodereviewsystem/coach/CoachStreamIntegrationTest.java`
- `src/test/java/com/ziyi/leetcodereviewsystem/coach/CoachTutorReviewTest.java`
- `src/test/java/com/ziyi/leetcodereviewsystem/coach/TutorPolicyTest.java`
- `docs/IMPLEMENTATION_REPORT_1_1.md`
