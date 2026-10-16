# Product Spec 1.1 — local acceptance evidence

Updated October 15, 2026. This records reproducible **local** core-MVP acceptance, not production
readiness, a security certification, or a guarantee that an LLM never makes mistakes. PASS is scoped
to the scenarios and evidence stated below; independent user product/AI-quality sign-off is pending.
No production database, migration, deployment or push was performed.

## Verification runs

- Full backend: `./mvnw test` — 133 tests, zero failures/errors/skips.
- Frontend: `pnpm test` — 12 tests, zero failures; `pnpm build` succeeds.
- Real provider: `scripts/coach_real_smoke.py --real` and
  `scripts/coach_tutoring_followups.py --real`, with semantic inspection of actual synthetic replies.
- Catalog attack: `scripts/coach_catalog_injection_smoke.py --real`; original local context restored.
- Non-AI API: `scripts/coach_local_checks.py`; 24 read and 33 write samples. Local p95:
  read 3.48 ms, write 20.52 ms. These values are not a production SLA or a load test.
- Browser: 360px and desktop layouts, original text/recovery, two-editor conflicts, context retention,
  keyboard confirmation/save/navigation, growth time focus/info and Enter behavior.

Automated evidence is in CoachFlowIntegrationTest, CoachAcceptanceIntegrationTest,
CoachConcurrencyTest, CoachMigrationTest, CoachStreamIntegrationTest, CoachProviderTest,
CoachTutorReviewTest, TutorPolicyTest, the original V1/M1 tests and frontend tests.
Runtime reports under ignored `target/` contain synthetic records only, never API keys or passwords.

## AC01 — Cold start

**PASS — local smoke.** A fresh synthetic account in coach_real_smoke.py received a real tool-generated 30-minute catalog draft; no Journal existed before confirmation. Budget, assumptions, catalog metadata and reasons were inspected.

## AC02 — Draft revision and confirmation

**PASS.** CoachFlowIntegrationTest and CoachConcurrencyTest cover stale revisions, duplicate retries and real two-thread PostgreSQL confirmation. coach_local_checks.py exercised HTTP revision conflicts and repeated confirmation; one session/Journal persisted.

## AC03 — Real learning and persistence

**PASS — local scenarios.** Browser feedback/Journal persisted across refresh and backend restart/re-login. Three real-provider sessions recorded result, actual minutes, original notes and feelings. Official-link behavior is checked in Workspace and the URL allowlist tests; see browser notes below for limits.

## AC04 — Lightweight feedback

**PASS.** Checkbox-only save, nullable minutes/results and uncheck/recheck preserve source text, attempt identity and growth. Overflow/fractional minutes are rejected. Browser blank-duration feedback and source consistency were verified.

## AC05 — Original text, edits and conflicts

**PASS.** Original Unicode/HTML-as-text preserved; stale updates reject with 409; corrected feedback recomputes derived reviews. Two real browser editors exposed both versions; a deliberate merge saved both synthetic lines. Unsaved Journal and feedback restored after reload.

## AC06 — Partial sessions

**PASS — bounded scenarios.** HTTP local checks exercised pause/end/resume on the same session; automated same-day sessions keep separate Journals and confirmed deferral preserves completed records. Time is not used to merge sessions or auto-roll tasks; timezone/date boundaries have dedicated tests.

## AC07 — Hint quality

**PASS — fixed-sample smoke; user sign-off pending.** After tightening policy and adding bounded semantic review, Binary Search, Valid Palindrome and Climbing Stairs each completed real L1/L2/L3, another-small-hint and explicit L4 requests. Replies were semantically inspected: observations/local directions/invariants before L4, no complete code before explicit reveal. Concepts did not escalate; new attempts reset; missing context clarifies. The reviewer is probabilistic, not a proof against leakage; independent human product sign-off remains.

## AC08 — Personalization

**PASS — rule and sample scope.** Current results, due dates, low-sample guards, related patterns and explicit overrides drive drafts. Model revisions now retain server-built evidence IDs and source reason codes instead of claiming the user edited them. Real feedback-to-next-plan smoke completed; recommendations are heuristics, not an optimality claim.

## AC09 — Difficulty and review rules

**PASS.** Result-v1 rules cover 1/3/7-day starting intervals, consecutive independent growth to 30 days, reset after difficulty, feedback correction and timezone boundary. Explicit difficulty filters constrain new drafts; existing confirmed snapshots are preserved.

## AC10 — Flexible plans

**PASS.** Natural remaining-items requests create a preview only. Explicit confirmation retains completed/source data and original snapshots. UI supports remaining-item replacement with a concrete preview; feedback after preview invalidates stale confirmation.

## AC11 — Learning memory

**PASS — automated and local HTTP.** Explicit save is idempotent; transient fatigue is not permanent memory. Edit/delete retrieval epochs exclude old conversation and assistant-hint text; feelings are excluded when disabled. Local HTTP checks exercised memory CRUD; dedicated integration tests inspect provider input.

## AC12 — Interview planning and recap

**PASS — basic MVP scope.** A target date/budget yields up to three adjustable foundation/practice/review windows, without company-question promises. An unspecified target uses an explicitly labelled 14-day assumption, not a saved preference. Date-bounded recap includes actual source IDs/results/notes; natural today/this-week/last-week and explicit ranges are resolved in the user timezone.

## AC13 — Notebook

**PASS — local scenarios.** HTTP title/note search and source-text preservation passed. Same-day distinct sessions and notes-only growth are covered. Browser edited, revisited and restored Journal text; search responses ignore stale in-flight queries. Pagination is implemented with 20 records per page.

## AC14 — Growth vine

**PASS — local scenarios.** Derived growth does not double-count retries/rechecks and does not decay. At 360px, the independent time button expanded without navigating; keyboard focus exposed time, and Enter opened the matching Journal. Default dates, same-day nodes and stable session timezone were inspected.

## AC15 — Failures and recovery

**PASS — automated/socket/browser scenarios.** Actual SSE socket close after pending still completed/persisted one response; replay recovered it without another provider call. Client split UTF-8 and disconnected-stream tests pass. Provider quota/timeout errors, marked demo, durable request/draft reuse and database failure rollback are tested. Observed real quota failure preserved input and later retry succeeded. SSE streams status/final result, not individual generated tokens.

## AC16 — Ownership and tool validation

**PASS — tested route matrix.** CoachAcceptanceIntegrationTest exercises foreign reads and writes for sessions, items, attempts, Journals, drafts, changes, memories, messages and request status. Own aggregate/export queries remain scoped. HTTP CSRF/unauthenticated access, invalid IDs/budget/links and tool whitelist are covered. Fixed local checks independently exercised cross-account HTTP reads/writes.

## AC17 — Prompt injection

**PASS — tested attack samples.** Real user/notes attack follow-ups did not reveal L4, complete attempts or create extra pages. A malicious local catalog-context sample returned only a small observation; compare-and-restore verified the original reference row was restored. Server tests independently reject unauthorized tools and forged contexts; model text cannot confirm plans, export another owner, run SQL/shell or use arbitrary network tools. This does not assert universal prompt-injection immunity.

## AC18 — Provenance

**PASS — repository/source inspection.** Nine default Coach entries use factual official metadata/links and independently authored teaching context/order. Source/version/provenance is attached per entry. Premium/archived/bad-link candidates are excluded by tests. No scraping, paid statements, copied solutions, cookie collection or unverified external dataset was added. Existing M1 licensed metadata is preserved separately.

## AC19 — Accessibility and layout

**PASS — sampled layout/keyboard checks; certification not claimed.** 360px Journal/Growth had scrollWidth=clientWidth=360; all visible buttons measured at least 44px. Desktop checks also showed no horizontal overflow. Keyboard save, confirmation, focus time, time button and navigation were exercised; labels/status/alerts/reduced-motion rules inspected. Full external screen-reader/WCAG certification was not performed.

## AC20 — Compatibility and migrations

**PASS — isolated local baseline.** Existing V1/M1 tests pass. Synthetic PostgreSQL schema upgrade retains legacy fields/history and creates no fabricated sessions; changes are additive V1–V5 with Hibernate validate. Browser records survive server restarts. Legacy unowned /problems and /stats are denied by default in the new authenticated app; explicit local compatibility mode preserves their code/URLs/semantics. This intentional access boundary is not an anonymous V1 deployment compatibility claim.

## AC21 — Real provider vertical smoke

**PASS — development account.** Real API chat → strict catalog/plan tool → draft → explicit confirm → current-problem Hint/concepts → feedback → next draft completed. Reports in target contain synthetic transcripts; no demo counted as AI. Subsequent review-enabled three-problem and follow-up runs completed. Final browser check after backend restart: the synthetic learner saw the thinking status, then a real two-sentence stack/queue explanation; the send button recovered and the sent input cleared.

## AC22 — Data rights

**PASS — disposable local account.** Owner-scoped full export and cascading account deletion tested both in integration and actual local HTTP. Deleted disposable credentials no longer log in; the second account remains accessible. Derived state is queried from owned sources. This setup has no automated backup retention policy; manual backups remain outside immediate deletion and require operator management.

## AC23 — Persistent Coach

**PASS — local browser.** The application shell retained conversation and unsent input across Workspace, Journal and Growth; refresh/re-login restored owned history. Historical-conversation selection and explicit new conversations are available. Server resolves current session items, attempt, notes and context; stale responses retain send-time identity.

## AC24 — Workspace

**PASS — implementation and browser scope.** Each item has a direct route, metadata/pattern disclosure, official target=_blank noopener link, same Coach, hints/feedback and return navigation. Browser source feedback matched Journal; no external editor read or code execution exists. Invalid item routes show an error instead of a different session.

## AC25 — Free tutoring

**PASS — fixed real samples; user sign-off pending.** Real follow-ups covered small hint, sorted-character misconception, general sorting complexity, a pasted Java indexing bug and an unrelated stack/queue question. Semantic review limits unsolicited replacement strategies; concept/debug did not advance levels or mark completion. A model can still be wrong or too verbose: original messages remain reportable and corrections persist.

## AC26 — Context race

**PASS — real concurrency, synthetic provider.** A controlled delayed provider call ran on a separate thread while B feedback changed in PostgreSQL; the eventual reply/hint stayed on A, preserving B. Foreign/conflicting IDs reject, new attempts reset, and client attribution/identity guards prevent cross-page or cross-account reply writes. Delay was synthetic to make the race reproducible, not claimed as a real-provider delay measurement.

## AC27 — Shared records and future context

**PASS.** Workspace/Journal use the same versioned attempt. Browser and HTTP reads matched source feedback exactly. New Journal text and actual hint replies reach authorized next context; notes are excerpted only in provider input, never overwritten in storage. Model-plan evidence, corrected results and review derivation are tested; hint-only requests never imply solved.

## Limits and sign-off

A reproducible core learning loop and bounded acceptance evidence now exist. User sign-off on the
actual tutoring samples and product experience is still appropriate; do not call agent semantic
inspection an independent human review. No exhaustive adversarial proof, external accessibility
certification, production load test, public-host auth hardening or backup policy is claimed.

The application streams request state and a persisted final reply; it does not stream model tokens.
Local test fixture schemas and synthetic accounts may remain for inspection. The real smoke scripts
are opt-in and incur API usage; repeat them deliberately, not on every unit-test run.

Later remains automatic reports/reminders, mock interviews, guaranteed company questions, LeetCode
sync, code execution, voice, payments/social, deployment and production hardening. These have not
been represented as implemented features.
