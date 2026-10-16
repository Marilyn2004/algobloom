# Coach core — local development and verification

Status: runnable local core MVP with real-provider vertical and three-problem smoke evidence.
Independent user product/AI-quality sign-off remains; production deployment is not included.
See [AC01–AC27 evidence](ACCEPTANCE_1_1.md). Product Spec 1.1 in
[PRODUCT_SPEC_1_1.md](PRODUCT_SPEC_1_1.md) supersedes the older milestone-only scope in `V2_SPEC.md`.
The user explicitly approved implementation; the copied source document retains its original header.

## Repository audit and reuse

The actual repository is the existing Spring Boot application, not a replacement project.
`Problem`, `Pattern`, catalog queries, legacy CRUD, review sessions, scheduling and the original tests
were reused. The baseline had no frontend, authentication, LLM integration, migration chain, or session
Journal. New code lives in the `coach` package and `frontend/`. Controllers delegate to learning/chat
services; `CoachStore` owns parameterized SQL access. Original entities and review fields remain intact.

Identity uses Spring Security, BCrypt, server-side HTTP sessions and CSRF tokens. New user-owned tables
include an owner FK and every lookup/query is scoped to the authenticated principal. Catalog metadata
is shared reference data. Frontend-supplied user IDs are never accepted as authority.

## Prerequisites

Java 21, local PostgreSQL, Node 22/24, and pnpm. The frontend lockfile pins the resolved React/Vite
versions. No Docker or external database service is needed. Local defaults use the existing PostgreSQL
role `marilyn` with an empty password; adapt **only the local datasource beans** if your local role differs.

Create dedicated databases once (do not point either name at valuable data):

```bash
createdb -h 127.0.0.1 -p 5432 leetcode_coach_dev
createdb -h 127.0.0.1 -p 5432 leetcode_review_test
```

Start the backend in the local-only profile:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments='--spring.profiles.active=coach'
```

`CoachLocalConfiguration` constructs a datasource for **127.0.0.1:5432/leetcode_coach_dev**; external
Spring datasource URL/user/password variables cannot redirect it. `@FlywayDataSource` also makes
migration use this exact pool, ahead of separate Flyway connection properties. The web server binds
loopback. This is a local development profile, not a production configuration.

Without a key, free AI requests return a clear unavailable error. Deterministic draft generation
remains usable. For clearly labelled UI demo replies, explicitly add `--coach.demo=true`. Demo
replies are not counted as real tutoring evidence and cannot satisfy real-AI acceptance.

Start the frontend in a second terminal:

```bash
cd frontend
pnpm install --frozen-lockfile --ignore-scripts
pnpm dev --port 5173
```

Open http://127.0.0.1:5173/coach. The Vite proxy sends only `/api` to the local backend. Register a
local account (username: letters/digits/underscore/hyphen; password at least 12 characters), then try:

1. “今天 30 分钟，不知道练什么” or the explicit rule-based draft button.
2. Adjust the draft and save the new revision; explicitly confirm the current revision.
3. Enter a problem Workspace, open its official link in a new tab, return and discuss the problem.
4. Record completion, result, optional time, original notes and feelings. Completion is not “solved”.
5. Open the session Journal, edit it, search the notebook, and inspect the positive growth timeline.
6. Pause/end the session or preview deferring remaining items; no change is applied without confirmation.
7. Manage preferences/memory and request a bounded-time recap in Settings.

## Configure a real AI provider

The backend adapter calls OpenAI Responses API at the fixed official HTTPS endpoint, with `store:false`.
Only the backend sees the key. API account access and billing are configured separately on the API
platform. No provider retention guarantee is made; `store:false` is not a zero-retention promise.

Create a development key on the OpenAI platform. In zsh, hidden input avoids placing it in shell history:

```bash
read -s 'COACH_OPENAI_API_KEY?Development API key (hidden): '
export COACH_OPENAI_API_KEY
printf '\n'
./mvnw spring-boot:run -Dspring-boot.run.arguments='--spring.profiles.active=coach'
```

`COACH_OPENAI_MODEL` selects a model available to your API project; the configurable default is
`gpt-4.1-mini`. Never put a key in Git, a frontend environment variable, a URL, or this chat.
`.env.example` documents variable names; `.env` is ignored and is not automatically loaded by Spring.
Stop the backend and `unset COACH_OPENAI_API_KEY` when finished.

A successful provider smoke must exercise actual chat → strict tool call → draft → explicit confirm →
current-problem tutoring/hint → feedback → another recommendation. Local HTTP stub tests verify the
wire protocol, not model quality. A missing key is an unaccepted test, not a passed mock equivalent.

## Rules and persistence

- Drafts have revisions, expiry and persistent cancellation. Confirmation locks the draft and creates
  exactly one session, its items/attempts and its unique Journal transactionally. The model has no
  confirmation tool. Request IDs and draft uniqueness protect retries.
- Plan reasons/evidence are stored with snapshots. Catalog IDs, official URLs, duplicates and budget
  are revalidated on the server. Confirmation-time metadata is resolved from the database.
- New V2 review rules use user-reported results: understanding/solution 1 day, hinted 3 days,
  independent 7 days; subsequent independent repeats double up to 30, difficulty resets the interval.
  Dates use the user's timezone and original completion instant. V1's 1/3/7/14/30 schedule is unchanged.
- Topic status remains `still_learning` below three valid attempts on at least two distinct problems.
  Recent independent results without recorded hints may support a modest increase; recorded assistance
  remains separate evidence, never silently overwrites a self-reported result.
- Derived skills/reviews are rebuilt from current attempts, so editing feedback does not double-count.
- Workspace and Journal use the same attempt. Version conflicts require explicit reconciliation. Original
  text remains separate from AI messages. Unsaved drafts are cached in the current browser for recovery.
- Conversations persist in PostgreSQL; an application-level React shell preserves the conversation and
  unsent input across routes. Requests and assistance bind to the send-time item/attempt/revision.
- Memory is explicit, editable and deletable. Deletion/edit sets a retrieval epoch; older conversational
  text is excluded from future model input rather than reintroducing deleted preferences via summaries.
  Recorded feelings are excluded by default and when analysis is disabled. User messages typed directly
  into Coach are sent as messages, so do not paste secrets.
- Growth is derived from source records, not incremented on retries. Notes/valid assistance count as
  activity; first completion adds learning leaves. Unchecking/editing does not duplicate growth.

## LLM and safety boundaries

Only strict `search_catalog`, `get_learning_context`, and planning-only `propose_plan` tools exist.
Candidate IDs are checked; no SQL, shell, unrestricted HTTP, identity switching, result writes or
confirmation tool is exposed. Directly recognized explicit memory/pause commands produce application
receipts, not fake AI replies. Remaining-plan commands create a confirmation preview only.

Current-interaction reveal permission is conservative and attempt-specific. “再提示一点” is not consent
for a solution. Concepts/debugging do not mechanically raise Hint levels. Original teaching contexts
are supplied, with clarification required when insufficient. This is **not a proof against LLM answer
leakage**: real three-problem and adversarial samples have been inspected; independent user quality sign-off remains. The server uses a structured semantic review, at most one rewrite/review retry, and a shared deadline. Rejected or unchecked output is replaced by an explicitly application-authored clarification and does not increment Hint state.

Limits: 64 KiB request bodies (including chunked input), 12,000-character user messages, configurable
provider context/output limits, at most five tool calls (including bounded semantic review) and one ordinary tool-result round within a shared
45-second deadline, default 50 messages/user/UTC-day, and a local auth rate limit. These are bounded
usage controls, not a measured monetary budget or production SLA. Only status, elapsed time, token counts and configurable cost estimates are logged; raw notes/code/keys are not logged. Input/output estimate rates default to 0.40/1.60 USD per million tokens for the default model and must be adjusted when changing model. Actual billing comes from the provider.
React renders text rather than trusting HTML/Markdown; official links are allowlisted and use
`noopener noreferrer`. The app never runs pasted code or reads the LeetCode editor.

## Migrations and old data

Flyway V1 creates the existing M1 schema only where absent; V2 adds user-owned coach tables; V3 adds
persistent cancellation flags; V4 adds owner-scoped, idempotent tutoring issue reports; V5 adds durable processing leases and message-to-draft recovery links. No drop/recreate, truncate, Flyway clean, or Hibernate automatic update
is executed. Hibernate now validates. Flyway is disabled outside explicit local/test configuration.

V1 rows/review history stay unchanged and unassigned: they are not fabricated into sessions or claimed
by the first registering account. A synthetic schema upgrade test verifies fields/history survive and
no fake Journals appear. Isolated synthetic fixture schemas are retained for inspection, not dropped.

Legacy `/problems` and `/stats` contain unowned personal state, so they are denied by default in this
new app. Their code, URLs and historical semantics remain available via the explicit **local-only**
compatibility flag `--coach.legacy-api-enabled=true`; regression tests set it on synthetic test data.
Do not enable that flag on a network service. Shared metadata-only catalog endpoints remain available.

Before any separately authorized production rollout: take/verify a database backup, validate the
baseline against a restored copy, review migrations/ownership mapping and establish backups/TLS/session
policy. Application rollback must not delete new tables; retain schema/data and roll forward with a
reviewed migration. No production migration, data access, push or deployment was performed here.

## Tests

```bash
./mvnw test
cd frontend
pnpm test
pnpm build
```

Every full Spring test imports `LocalTestDatabaseConfiguration`; both JPA and Flyway use its explicit
loopback test pool. Do not remove the import. New concurrency/migration fixtures also use only the same
literal local test database with separate synthetic schemas. Test properties use `validate`, not
`create-drop`. A future full context omitting the safety import must not be accepted.

Real AI samples, browser recovery/conflict checks, actual socket-disconnect recovery and local read/write p95 are documented separately in the acceptance checklist. These checks are not universal LLM safety, accessibility certification or a production SLA.

## Data rights and deferred work

Export returns only the authenticated user's records, including full Journal history. Account deletion
removes that user's originals, memory and messages through FKs; derived views no longer retrieve them.
This local setup has no automated backup retention policy; any manual backups are outside immediate
application deletion and must be managed separately. Shared-device users should clear browser site data.

Not yet accepted MVP work is listed separately in the acceptance checklist. Later remains automatic
reports/reminders, mock interviews/company-specific guarantees, LeetCode sync, code execution, voice,
advanced models, payments/social features, and production deployment/hardening.

## Convenient local startup and acceptance scripts

After configuring the backend key in your terminal, run from the repository root:

```bash
bash scripts/run-local-coach.sh
```

It selects installed Java 21 on macOS, never prints/saves the key and explicitly selects the fixed
local profile and real-provider mode. The frontend still starts separately as described above.

The authenticated `POST /api/v2/conversations/{id}/messages/stream` sends SSE status/final-result
messages. Existing synchronous POST remains available. A broken stream does not cancel persisted
work: GET `/api/v2/conversations/{id}/requests/{requestId}` recovers status/result, and the same
request ID reuses its original draft/reply. Pending input/request IDs survive browser reload.

Opt-in local acceptance commands (real-provider commands incur API usage):

```bash
python3 scripts/coach_local_checks.py
python3 scripts/coach_real_smoke.py --real
python3 scripts/coach_tutoring_followups.py --real
python3 scripts/coach_catalog_injection_smoke.py --real
```

All script API/database targets are literal loopback development targets. The catalog attack script
briefly changes one local reference context and restores it with compare-and-restore protection;
run it only in this isolated development environment. Synthetic source records are retained except
for the disposable account explicitly deleted by the data-rights check. No test writes to Railway.
