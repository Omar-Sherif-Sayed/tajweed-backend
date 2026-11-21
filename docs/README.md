# Documents of tajweed-backend

Only what concerns this codebase. Everything about the product as a whole (rules, decisions, sources, permissions,
design, the plan) is in the documentation repository: <https://github.com/Omar-Sherif-Sayed/tajweed-docs>.

| Path | What | Since |
| --- | --- | --- |
| [`api/openapi.yaml`](api/openapi.yaml) | The OpenAPI contract the web, Android and iPhone clients read. **Generated** by `./gradlew integrationTest` from the code — never edited by hand; CI fails when the committed file is out of date. | 2026-11-21 |
| [`database/README.md`](database/README.md) | Every table and column, and where each value comes from; the migrations in order. | 2026-11-21 |
| [`data-sources/README.md`](data-sources/README.md) | Every content file of this repository: origin, licence, SHA-256, checks; how a new one is added. | 2026-11-21 |
| [`../AGENTS.md`](../AGENTS.md) | For agents and developers: state, rules, versions, structure, conventions with examples, how to build and run. | 2026-11-21 |
| [`../README.md`](../README.md) | For people: what the API does, how to run, test and deploy it. | 2026-11-21 |

Planned folders, created with their first document: `deployment/` (the hosting guide), `quality/` (acceptance runs).
