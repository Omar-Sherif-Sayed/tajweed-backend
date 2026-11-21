# tajweed-backend

The public, read-only REST API of **تجويد - القرآن الكريم وعلومه — Tajweed - The Noble Quran and Its Sciences**:
a free Islamic application for the web, Android and iPhone. No login, no accounts, no advertisements, no tracking.
It serves verified content to the three clients in 22 languages.

The whole product — its rules, decisions, design and plan — is described in the documentation repository:
<https://github.com/Omar-Sherif-Sayed/tajweed-docs>.

## What it does today

| Endpoint | What it returns |
| --- | --- |
| `GET /v1/app-info` | the application's name, author, API version and the 22 languages, in the language of `Accept-Language` |
| `GET /v1/surahs` | the 114 surahs: Arabic and transliterated names, Makki / Madani, order of revelation, ayahs, first page |
| `GET /v1/surahs/{surah}/ayahs` | a surah and all its ayahs — the text exactly as Tanzil writes it — and the text's source |
| `GET /v1/surahs/{surah}/ayahs/{ayah}` | one ayah with its page, juz, hizb, quarter and sajdah |
| `GET /v1/pages/{page}` | the ayahs that start on a page of the Madinah mushaf (1 – 604) |
| `GET /v1/juzs` | where each of the 30 juz starts |
| `GET /v1/quran-search?q=…` | one search box: words with or without tashkeel (the ayahs that contain them, with the words to highlight, page by page), a surah name (Arabic or Latin), or a reference (`2:255`, «البقرة 255») |
| `GET /v1/reciters` · `GET /v1/reciters/{id}` | the reciters, each with his recitations: the riwayah said plainly (its name, rawi and qari; *listening only* when it is not Hafs, the riwayah of the displayed text), the style, how many surahs, whether ayah timings exist |
| `GET /v1/recitations/{id}` | one recitation: the surahs it holds, its audio sources in the order a player tries them (with their daily health check), its credits |
| `GET /v1/recitations/{id}/surahs/{surah}/timings` | when each ayah starts and ends in the surah file (mp3quran's timings, checked against the Tanzil ayah count) |
| `GET /v1/content-version` | a fingerprint of the content's sources — the apps refresh their copies when it changes |
| `GET /actuator/health` | `UP` when the application and its database are working |
| `GET /swagger-ui.html` · `GET /v3/api-docs` | the API's documentation and its OpenAPI contract |

Every answer is wrapped in the same envelope:

```json
{ "success": true, "code": "OK", "message": "Success", "data": { "...": "..." } }
```

An error has `"success": false`, an error `code` (for example `NOT_FOUND_PATH`) and a `message` in the requested
language. The language comes from the `Accept-Language` header — `ar` (the default), `en`, `ru`, `es`, `de`, `it`,
`fr`, `ur`, `hi`, `ko`, `id`, `ja`, `zh`, `tr`, `bn`, `fa`, `ms`, `sw`, `ha`, `pt`, `ps`, `uz`.

## The Quran text

The text is the **Tanzil Quran Text (Uthmani, 1.1)** — <https://tanzil.net> — under Creative Commons Attribution 3.0:
it is copied verbatim and never changed. The file is checked by its SHA-256 before it is imported, and a test compares
every stored ayah with it. Page, juz and quarter positions come from Tanzil's metadata (the quarter at 15:49 follows
the printed Madinah mushaf). Details: `docs/data-sources/README.md` and `docs/database/README.md`.

## Built with

Java 25 · Spring Boot 4.2 · PostgreSQL 18 with Flyway · springdoc-openapi · Bucket4j (rate limiting) · JUnit, AssertJ,
Mockito, Testcontainers, ArchUnit · Gradle. The versions and the reason for each library are in `AGENTS.md` and in
the documentation repository's `docs/plan/backend-plan.md`.

## Run it on your computer — step by step

1. Install **JDK 25** and **Docker Desktop**, and start Docker Desktop.
2. Get the code: `git clone git@github.com:Omar-Sherif-Sayed/tajweed-backend.git` and open the folder.
3. Start the application: `./gradlew bootRun` (on Windows `gradlew.bat bootRun`). The first time, Gradle downloads
   itself and the libraries. Spring Boot then starts a local PostgreSQL 18 in Docker (from `compose.yaml`), creates
   the tables with Flyway and listens on port 8080.
4. Try it: open <http://localhost:8080/v1/app-info>, or with a language:
   `curl -H "Accept-Language: en" http://localhost:8080/v1/app-info`.
5. The documentation page: <http://localhost:8080/swagger-ui.html>.
6. Stop with `Ctrl+C`; `docker compose stop` stops the local database (its data stays in a Docker volume).

On the author's Windows machine two settings are needed first — see `AGENTS.md`, "Build, test, run".

## Tests

| Command | What it runs |
| --- | --- |
| `./gradlew build` | compiling, the unit tests (no database), the Javadoc checks, the architecture rules |
| `./gradlew integrationTest` | the tests on a real PostgreSQL 18 in Docker; it also rewrites `docs/api/openapi.yaml` |
| `./gradlew jacocoTestReport` | the coverage report: `build/reports/jacoco/test/html/index.html` |

Every class, method and field carries a Javadoc comment; the build fails when one is missing.

## Run it in production

Build the image with `docker build -t tajweed-backend .` and run it with the database settings as environment
variables. The settings:

| Variable | Meaning | Default |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | the database | — (required) |
| `CORS_ORIGINS` | the web application's addresses, comma-separated | `http://localhost:4200` |
| `RATE_LIMIT_PER_MINUTE` | requests one client may send each minute | `120` |
| `AUDIO_HEALTH_ENABLED`, `AUDIO_HEALTH_CRON`, `AUDIO_HEALTH_TIMEOUT` | the daily check of the audio sources: on or off, when (Spring cron, UTC), how long one request may take | `true`, `0 30 3 * * *`, `20s` |
| `SERVER_FORWARDEDHEADERSSTRATEGY` | `native` when the API runs behind a reverse proxy | `none` |
| `SWAGGER_UI_ENABLED`, `API_DOCS_ENABLED` | the documentation page and the OpenAPI document | `true` |

No secret is ever written in this repository; it is public.

## Found a mistake?

Open an issue: <https://github.com/Omar-Sherif-Sayed/tajweed-backend/issues>. The texts in the 21 languages other than
Arabic are translated by the project; corrections from readers are welcome.

## Licence

The code is under the MIT licence (`LICENSE`). Content served by the API keeps its own licence, named in the
application's Sources section.
