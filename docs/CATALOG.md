# AlgoBloom — catalog provenance and compatibility

This document preserves the M1 metadata attribution and API contract. AlgoBloom’s Coach uses an
independently authored nine-problem teaching path from `CoachCatalogInitializer`, rather than
copying the upstream ordering. See [current setup](CORE_MVP.md).

## Source and permission basis

The 12 manually selected entries in `src/main/resources/catalog/problems.tsv` use metadata from
[neetcode-gh/leetcode `.problemSiteData.json`](https://github.com/neetcode-gh/leetcode/blob/main/.problemSiteData.json),
reviewed on 2026-10-06. The upstream repository explicitly distributes its software and associated
files under the [MIT license](https://github.com/neetcode-gh/leetcode/blob/main/LICENSE),
Copyright (c) 2022 neetcode-gh. That license permits use, modification and redistribution subject
to retaining the notice. The complete upstream notice is included as
`src/main/resources/catalog/NEETCODE_LICENSE.txt` and packaged with the resource.

Only titles, difficulty labels, problem link slugs, numeric identifiers (from the upstream `code`
field), and a legacy classification are retained. URLs are constructed from the slugs. Canonical
pattern assignments are independently curated for this project; they are not a claim to reproduce
LeetCode's official topic taxonomy. No problem statements, examples, solutions, videos, or source
code are bundled. The application makes no calls to LeetCode or to the upstream repository.
The MIT permission basis applies to the selected upstream metadata, not a license to LeetCode's
proprietary statements or other content. Any future source requires separate provenance/license review.

## Contents

12 problems: 6 Easy, 5 Medium, 1 Hard. Canonical patterns: Arrays, Hash Table, Two Pointers,
Sliding Window, Binary Search, BFS, DFS, Backtracking, Dynamic Programming,
Heap / Priority Queue, and Prefix Sum.

The resource is a small tab-separated file with eight columns. `patterns` contains semicolon-separated
`slug:display name` pairs. `legacyPattern` is explicitly supplied separately; it is never synchronized
with canonical patterns. To expand the catalog, add reviewed rows and update this provenance document.
There is no generic ingestion framework.

## APIs

- `GET /catalog/problems`: catalog entries, ordered by internal ID ascending.
- `GET /catalog/problems/{id}`: catalog detail by internal ID; 404 for missing or legacy-only records.
- `GET /catalog/patterns`: persisted patterns, ordered by slug.

Optional list filters use exact matches: `difficulty=Easy|Medium|Hard` and `pattern=<canonical-slug>`.
Unknown filter values return an empty list. Filters combine with AND.

```bash
curl 'http://localhost:8080/catalog/problems?difficulty=Medium&pattern=hash-table'
```

Problem responses contain `id`, `platform`, `externalProblemId`, `title`, `slug`, `difficulty`,
`url`, and `patterns` (each with `id`, `slug`, `name`). They contain no legacy learning/review state.
IDs are database-assigned, not LeetCode IDs. Identified catalog records have all four new identity/link
fields populated. Legacy unidentified rows remain nullable and are excluded from catalog reads.

## Current local setup and compatibility

Use the dedicated local development/test databases and startup instructions in [CORE_MVP.md](CORE_MVP.md).
Hibernate now uses `validate`; additive Flyway migrations V1–V5 are enabled for local/test use.
The former M1 `ddl-auto=update` / test `create-drop` instructions no longer apply.

The M1 loader remains opt-in (`local` profile plus `catalog.seed.enabled=true`) and disabled by
default. It is separate from the `coach` profile’s teaching-path initializer. Do not use the M1
startup command as the current application setup. Loading requires an already prepared compatible
local schema; its loopback check runs after persistence startup, not as a pre-Hibernate guard.

The legacy singular `Problem.pattern` remains separate from canonical `Problem.patterns`.
Legacy POST ignores supplied IDs, and PUT preserves catalog identities/associations while retaining
its existing title/difficulty update semantics. V1 scheduling and history remain implemented.
Legacy private `/problems` and `/stats` access is denied by default; the explicit local compatibility
flag and regression tests are documented in the current guide. Catalog responses expose metadata only.

Keep `problems.tsv`, `NEETCODE_LICENSE.txt`, the migrations and existing tests for provenance,
compatibility and reproducibility. Do not run loaders, schema changes or tests against production.
