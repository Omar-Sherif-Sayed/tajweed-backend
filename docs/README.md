# Documents of tajweed-backend

Only what concerns this codebase. Everything about the product as a whole (rules, decisions, sources, permissions,
design, the plan) is in the documentation repository: <https://github.com/Omar-Sherif-Sayed/tajweed-docs>.

| Path | What | Since |
| --- | --- | --- |
| [`api/openapi.yaml`](api/openapi.yaml) | The OpenAPI contract the web, Android and iPhone clients read. **Generated** by `./gradlew integrationTest` from the code — never edited by hand; CI fails when the committed file is out of date. | 2026-11-21 |
| [`api/client-guide.md`](api/client-guide.md) | The phase 1 contract for the clients (`P0-14`): every call, caching, errors, the offline Quran bundle, the reader, search, playback and failover. | 2026-11-22 |
| [`setup/quran-foundation-keys.md`](setup/quran-foundation-keys.md) | How the author keeps the Quran Foundation's keys in his environment — never in a file — and how the agent tests with them (`./gradlew liveTest`). | 2026-11-22 |
| [`database/README.md`](database/README.md) | Every table and column, and where each value comes from; the migrations in order. | 2026-11-21 |
| [`data-sources/README.md`](data-sources/README.md) | Every content file of this repository: origin, licence, SHA-256, checks; how a new one is added. | 2026-11-21 |
| [`../AGENTS.md`](../AGENTS.md) | For agents and developers: state, rules, versions, structure, conventions with examples, how to build and run. | 2026-11-21 |
| [`../README.md`](../README.md) | For people: what the API does, how to run, test and deploy it. | 2026-11-21 |

Planned folders, created with their first document: `deployment/` (the hosting guide), `quality/` (acceptance runs).
