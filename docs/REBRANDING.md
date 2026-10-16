# AlgoBloom branding and repository transition

Product: **AlgoBloom** — **Your Personal AI Coding Interview Coach**.
Short tagline: **Practice smarter. Grow with every problem.**
The persistent AI Coach is the product; plans, practice, journals and growth support it.

## Local changes and cleanup

The frontend title, description/Open Graph metadata, header, login screen, footer and private npm
package name use AlgoBloom. A local SVG sprout favicon extends the existing cream/green design.
No application flows, authentication, API shapes or database behavior were changed.
README now documents the actual coaching MVP, local startup and deployment limitations. Stale M1
loading/schema instructions were replaced with current-guide links; attribution and compatibility
contracts remain. Historical reports are labelled as snapshots instead of silently rewriting evidence.
No historical files, migrations, tests or licenses were deleted.

## Retained identifiers

- Java package `com.ziyi.leetcodereviewsystem`, application main class and Maven artifactId.
- Spring application name, PostgreSQL database names, schemas/tables, migrations V1–V5.
- Existing API paths, profile `coach`, script names, `COACH_*` variables and browser storage keys.
- Official LeetCode names/links and upstream copyright attribution: these identify external content,
  not AlgoBloom’s product identity. Approved specs retain their original wording.
- Local folder remains `leetcode-review-system`; origin now uses `https://github.com/Marilyn2004/algobloom.git`.
  The existing Railway URL/service/project are intentionally unchanged.

Renaming these is unnecessary for product branding and could break compatibility, persistence,
scripts or deployment assumptions. Any future change requires a targeted transition plan.

## Completed GitHub transition

The repository is now [Marilyn2004/algobloom](https://github.com/Marilyn2004/algobloom).
Its numeric repository ID remains `1289247419`, preserving repository identity and Git history.
Authenticated admin access was verified before renaming. The local origin uses the new URL.
The default branch remains `main`; the current AlgoBloom README and MVP are on
`codex/v2-core-mvp`. No merge or push to `main` was performed.

GitHub description:

> AlgoBloom — Your Personal AI Coding Interview Coach. Persistent AI guidance, adaptive practice,
> progressive hints, and a reflective learning loop with Spring Boot, React, and PostgreSQL.

Topics: `algobloom`, `ai-coach`, `coding-interview`, `interview-preparation`, `spring-boot`,
`java`, `react`, `typescript`, `postgresql`, `openai`, `spaced-repetition`.
Homepage: https://algobloom-demo.onrender.com/coach

## Deployment integration verification

The user explicitly authorized repairing repository connections without deploying or touching databases.
Railway automatically updated both the backend source and existing deployment trigger to
`Marilyn2004/algobloom`. No Railway mutation was needed. Project `kind-creativity` retains its
single `production` environment, backend `leetcode-review-system`, and `Postgres` service.
The trigger still watches `main`; `prDeploys=false`.

Before and after the rename, latest deployment IDs were unchanged:

- Backend: `d6bd0423-0dc6-488c-b028-113992f6febc` (`SUCCESS`).
- PostgreSQL: `48f05b4a-f0e8-423f-b5c5-169326c7cc59` (`SUCCESS`).

Render service `algobloom-demo` was explicitly updated to the new repository URL and retained
branch `codex/v2-core-mvp` with automatic deployments disabled. This was a connection update,
not a deployment request. The public demo continues using its separate Neon database.
No database operation, production deployment, merge, or push to `main` was performed.

## Validation

The last application-code validation passed 137 backend tests and 12 frontend tests, TypeScript
checks, and the production frontend build. Public deployment verification passed nine synthetic
workflow checks with real AI responses; details are in [FREE_DEPLOYMENT.md](FREE_DEPLOYMENT.md).
This repository-transition follow-up changes documentation and Git/deployment connection metadata
only. Git diff and staged-secret checks are performed before committing it.
