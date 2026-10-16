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
- Local folder and current Git remote remain `leetcode-review-system` until a separately approved
  transition. The existing Railway URL/service/project are intentionally unchanged.

Renaming these is unnecessary for product branding and could break compatibility, persistence,
scripts or deployment assumptions. Any future change requires a targeted transition plan.

## GitHub audit and proposed metadata

Current public repository: `Marilyn2004/leetcode-review-system`; default branch `main`.
The original tracker description and empty topics have been replaced with the AlgoBloom metadata
below. Desired repository name remains `Marilyn2004/algobloom`.
The public target URL returned 404; this is not proof that the name is available (it might be private,
reserved or otherwise unavailable). Authenticated GitHub API access confirms the account has repository admin permission.

Current GitHub description:

> AlgoBloom — Your Personal AI Coding Interview Coach. Persistent AI guidance, adaptive practice,
> progressive hints, and a reflective learning loop with Spring Boot, React, and PostgreSQL.

Current GitHub topics: `algobloom`, `ai-coach`, `coding-interview`, `interview-preparation`, `spring-boot`,
`java`, `react`, `typescript`, `postgresql`, `openai`, `spaced-repetition`.

Description and topics were updated and verified through authenticated GitHub API requests.
The repository name and local Git remote remain unchanged; no `algobloom` repository was created.

## Railway integration inspection and risks

Read-only authenticated inspection found project `kind-creativity`, one `production` environment,
backend service `leetcode-review-system` and `Postgres`. The backend repository trigger watches
`main` on `Marilyn2004/leetcode-review-system`; `prDeploys=false`. Its datasource references point
to production Postgres. No deployment or settings/database modification was performed.

Admin rights are verified. An authenticated target lookup returned 404, so no accessible target-name
conflict was found; reserved-name restrictions would still only be settled by GitHub’s rename operation.
Repository webhooks are empty and GitHub Actions reports zero workflows. The OAuth credential cannot
list GitHub App installations through the user-installations endpoint; Railway’s actual connection
was therefore inspected directly through its authenticated API.

**Rename paused for integration safety:** Railway still stores the old repository path in both the
production service source and deployment trigger. No verified guarantee was found that a rename
updates those settings automatically. Because production-setting edits are prohibited, the rename
was not attempted. This is an unresolved integration dependency, not proof that a rename must fail.
Obtain verified Railway support/documentation confirming rename handling, or separately authorize a
controlled reconnection plan, before attempting it. GitHub redirects alone are insufficient.

Once the integration dependency is resolved, rename on GitHub, update only the local origin URL, verify access and
reinspect Railway’s stored source/trigger. Any required Railway reconnection is separate work needing
a safe change window and authorization. Keep production on main and do not deploy this branch.

## Validation and next gate

Validation on October 15, 2026: backend 133 tests passed (zero failures/errors/skips); frontend
12 tests passed; TypeScript checks and production build passed. The browser confirmed the title,
metadata, favicon link and sign-in branding. At 360 px, document width remained 360 px without
horizontal overflow. The backend was not running during this frontend-only inspection, so its
connection-error state was visible; no new end-to-end AI run or usable product screenshot is claimed.
Diff/secret checks cover modified and untracked source/document assets; no production access occurred.
The user subsequently authorized branding commits and pushing only `codex/v2-core-mvp`. The initial
branding commit is `0061b95`; this follow-up records verified remote metadata and the rename blocker.
No merge/push to main, production deployment, database operation or Railway setting change occurred.
Independent user teaching-quality acceptance and production hardening remain separate from branding verification.
