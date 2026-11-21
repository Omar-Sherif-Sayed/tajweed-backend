# AGENTS.md — tajweed-backend

> **State (2026-11-21): phase 1 started.** Scaffold (`P0-07`) + the **`quran` module**: the Tanzil Uthmani text and
> metadata imported byte for byte after checksum and count checks (6,236 ayahs, 604 pages, 30 juz, 240 quarters with
> the reviewed 15:49 correction), served by `/v1/surahs`, `/v1/pages/{page}`, `/v1/juzs`; the `source` module with
> `GET /v1/content-version`. 57 tests green (every ayah compared with the file). Next: the search text (⏸ the
> author's permission to download Tanzil's Simple texts), the `reciter` module, the API contract for phase 1.

## Read first

This repository is one of five. The product's scope, the binding rules (correct data only, no advertisements, no
login, 22 languages), the decisions and the task list live in the **parent `AGENTS.md`** of the documentation
repository:

- in the working folder: `../AGENTS.md` (this repository is cloned inside the `tajweed-docs` folder);
- on GitHub: <https://github.com/Omar-Sherif-Sayed/tajweed-docs/blob/main/AGENTS.md>.

Read the parent file's §0 (state and next task), §1 (working agreement), §6 (data integrity rules), §8.14 (Dorar's
binding rules) and §9 (backend conventions) before any work here. The plan this scaffold follows, with the reason for
every library, is `docs/plan/backend-plan.md` of the documentation repository.

## Rules that apply to every change here

1. **Plan, ask, then code** (parent §1). A database change lists the exact tables and columns for the author's
   approval before the migration is written.
2. **Javadoc everywhere** (the author, 2026-11-21: «javadoc on the code everywhere, to be readable in the future»):
   every package (`package-info.java`), type, constructor, method and field of `src/main` has a Javadoc comment that
   says what it is for and why; every test class says what it proves, every test method has a `@DisplayName`.
   **The build enforces it**: Checkstyle (`config/checkstyle/checkstyle.xml`, Javadoc rules only) fails on a missing
   comment, and the Javadoc tool (`-Xdoclint:all,-missing -Werror`) fails on a broken one.
3. **Document everything**: after every task, update this file (state, structure, conventions), `README.md` and
   `docs/README.md`; the parent repository records the task's status.
4. **No secret in the repository** — it is public. Secrets only in environment variables (§ Settings below).
5. **Every user-facing text in the 22 languages**: a new message key goes into all 22 files of
   `src/main/resources/i18n/` (`MessageFilesTest` fails otherwise). Religious content is never translated by us
   (parent §6.10).
6. **Commits** as Omar Sherif with his address (set per repository), no AI co-author line; push at the end of a task.

## Versions (checked on Maven Central, postgresql.org and Docker Hub on 2026-11-21 — task P0-06)

| Part | Version | How it is set |
| --- | --- | --- |
| Java | 25 (Temurin in CI, Oracle JDK 25.0.5 on the author's machine) | Gradle toolchain |
| Gradle | 9.9.2 | wrapper |
| Spring Boot | 4.2.0 (Spring Framework 7.1.0, Spring Data 2026.1.0) | plugin |
| `io.spring.dependency-management` | 1.1.7 | plugin |
| Hibernate 7.4.15 · Flyway 12.11.0 · PostgreSQL JDBC 42.7.14 · Jackson 3.1.8 · Lombok 1.18.48 · Caffeine 3.3.1 | — | managed by Spring Boot (no version in `build.gradle`) |
| JUnit Jupiter 6.1.3 · AssertJ 3.27.7 · Mockito 5.24.0 · Testcontainers 2.0.5 | — | managed by Spring Boot |
| springdoc-openapi | 3.2.1 (built on Spring Boot 4.2.0) | `ext.springdocVersion` |
| Bucket4j | 8.22.0 | `ext.bucket4jVersion` |
| ArchUnit | 1.5.1 | `ext.archunitVersion` |
| Checkstyle | 14.4.0 | `checkstyle.toolVersion` |
| PostgreSQL | 18 (`postgres:18` image; 18.7 on 2026-11-21) | `compose.yaml`, `TestcontainersConfiguration` |
| Runtime image | `eclipse-temurin:25-jre` | `Dockerfile` |
| CI actions | checkout v7, setup-java v6, gradle/actions v6, upload-artifact v7 | `.github/workflows/ci.yml` |

Not yet added (each comes with its first use, with its version checked that day): ShedLock (first scheduler — radio
health checks), WireMock and the HTTP interface clients (first external API — mp3quran, Quran Foundation, Dorar).

## Structure

```
build.gradle                 dependencies (why each one: comments), test tasks, Javadoc checks, JaCoCo
config/checkstyle/           checkstyle.xml (main: Javadoc everywhere) · checkstyle-test.xml (tests)
compose.yaml                 local PostgreSQL 18 — started by bootRun itself
Dockerfile                   build on JDK 25, run on JRE 25 as a non-root user
.github/workflows/ci.yml     build + unit tests + Javadoc checks · integration tests · OpenAPI contract check
docs/                        README.md (index) · api/openapi.yaml (generated) · database/ (every table and column)
                             · data-sources/ (every content file: origin, licence, SHA-256)
src/main/java/io/github/omarsherifsayed/tajweed/
  TajweedApplication.java
  general/                   shared by every module; never depends on business/
    config/                  AppProperties (tajweed.*), WebConfig (CORS, ETag), OpenApiConfig
    filter/                  SecurityHeadersFilter, RateLimitFilter (Bucket4j per IP, 429 RATE_LIMITED)
    i18n/                    SupportedLanguages (22), LocaleConfig (Accept-Language → locale), MessageService
    exception/               ErrorCode, BaseException, GlobalExceptionHandler
    schema/                  BaseResponse<T>
    audit/                   AbstractAuditingEntity (created_at, updated_at)
  business/
    appinfo/                 GET /v1/app-info — controller · service (+ impl) · schema/response (Dto*) · mapper
    source/                  content_source: GET /v1/content-version (entity, repository, service, mapper)
    quran/                   surah, ayah: GET /v1/surahs, /v1/surahs/{n}/ayahs, /v1/surahs/{n}/ayahs/{a},
                             /v1/pages/{page}, /v1/juzs (entity, enums, repository + projections, services, mapper)
src/main/java/db/migration/  Java migrations that import content: V1_1_1__ImportQuranText (checksums, counts, insert)
src/main/resources/
  application.properties     every setting, with its environment variable
  db/migration/              V1_0_0__baseline.sql, V1_1_0__create_content_source_and_quran_tables.sql …
  content/                   source files, byte for byte (-text in .gitattributes): tanzil/quran-uthmani.txt,
                             tanzil/quran-data.xml, reviewed/hizb-quarter-corrections.csv — docs/data-sources/README.md
  i18n/                      messages.properties (Arabic, default) + messages_<code>.properties × 21
src/test/java/…              mirrors main; ArchitectureTest; ApplicationIntegrationTest (tag integration)
```

## Conventions — with the real code as the example

- **A module** = `business/<module>/` with `controller/<Entity>RetrievalController`, `service/<Entity>RetrievalService`
  (interface) + `service/impl/<Entity>RetrievalServiceImpl`, `repository/`, `entity/`, `schema/request/Req*`,
  `schema/response/Dto*` (records), `mapper/<Entity>Mapper` (`final`, static methods), `enums/`, `constant/`,
  `scheduler/`. The model: `business/appinfo/`.
- **Controller**: path, status and headers only. Example `AppInfoRetrievalController`: `@RequestMapping("/v1/app-info")`,
  takes the request's `Locale`, returns `ResponseEntity<BaseResponse<Dto…>>` with `Cache-Control` and
  `Vary: Accept-Language`.
- **Every answer is a `BaseResponse<T>`** (`success`, `code`, `message` in the request's language, `data`). No time
  stamp in the body: equal content gives an equal body, so the `ETag` works (a repeated request answers `304`).
- **Errors**: throw `new BaseException(ErrorCode.X)`; `GlobalExceptionHandler` answers with the code's status and its
  message in the request's language. A new code: add it to `ErrorCode` with its key, and the key to all 22 files.
  Codes are never renamed (the clients depend on them). Unknown path → `404 NOT_FOUND_PATH`; wrong method →
  `405 METHOD_NOT_ALLOWED`; bad input → `400 INVALID_REQUEST`; limit → `429 RATE_LIMITED`; anything else is logged
  in full and answered `500 INTERNAL_ERROR` without details.
- **Languages**: `Accept-Language` → one of the 22 (`SupportedLanguages`), region ignored (`zh-CN` → `zh`), otherwise
  Arabic. Texts only from `MessageService` / the message files.
- **Injection**: constructor injection with Lombok `@RequiredArgsConstructor`; no `@Autowired` fields in `src/main`.
- **Database**: the schema only through Flyway; `spring.jpa.hibernate.ddl-auto=validate`; an applied migration is never
  edited; every table has `pk_id` and the audit columns; every constraint is named. Content of Quran Foundation and
  Dorar is **never** committed or stored beyond their terms (parent §5, §7.1, §8.14).
- **Content import**: a Flyway **Java** migration in `db.migration` reads the files of `src/main/resources/content/`,
  verifies each SHA-256 against a constant, checks every count, and only then inserts — the text as written, never
  edited (see `V1_1_1__ImportQuranText`). Content entities are `@Immutable`; the code never writes them. A reviewed
  correction is a data file with its evidence (`content/reviewed/`), never a hidden `if` in code.
- **Every Quran answer carries `textSource`** (Tanzil's name and link — its licence requires them to be shown).
- **Logs**: identifiers and timings, never request bodies, secrets or IP addresses.
- **Tests**: given / when / then; class Javadoc + `@DisplayName`; unit tests need no database or network; integration
  tests are tagged `integration` and use `TestcontainersConfiguration` (PostgreSQL 18).
- **Architecture rules** (`ArchitectureTest`, every build): `general` never depends on `business`; controllers never
  touch repositories or entities; nothing outside `service.impl` uses an implementation; REST controllers live in
  `controller` and end in `Controller`; entities live in `entity`; mappers are `final`.

## Build, test, run (details for people: `README.md`)

On this machine the environment needs two adjustments before Gradle (checked 2026-11-21): `JAVA_HOME` must point to
JDK 25 (`C:\Program Files\Java\jdk-25.0.5.1`), and a short temporary folder avoids a Gradle daemon failure with the
long default `%TEMP%`:

```bash
mkdir -p /c/gtmp
export JAVA_HOME="C:\Program Files\Java\jdk-25.0.5.1" TEMP='C:\gtmp' TMP='C:\gtmp'
./gradlew build jacocoTestReport   # compile, unit tests, Checkstyle (Javadoc), Javadoc tool, coverage report
./gradlew integrationTest          # PostgreSQL 18 in Docker; also rewrites docs/api/openapi.yaml
./gradlew bootRun                  # local run; starts compose.yaml's PostgreSQL by itself (Docker Desktop running)
```

Commit `docs/api/openapi.yaml` whenever `integrationTest` changes it — CI fails when the committed file is out of date.

## Settings (environment variables)

| Variable | Meaning | Default |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | the production database | local: from `compose.yaml` |
| `CORS_ORIGINS` | the web application's origins, comma-separated | `http://localhost:4200` |
| `RATE_LIMIT_PER_MINUTE` | requests per client per minute on `/v1/**` | `120` |
| `SERVER_FORWARDEDHEADERSSTRATEGY` | `native` behind a reverse proxy (the client's address for the rate limit) | `none` |
| `SWAGGER_UI_ENABLED`, `API_DOCS_ENABLED` | the Swagger page and the OpenAPI document | `true` |
| (later) `DORAR_API_KEY`, `QF_CLIENT_ID`, `QF_CLIENT_SECRET`, `QF_ENV` | external APIs — never in a file | — |

## Verified on 2026-11-21

- `./gradlew build jacocoTestReport integrationTest`: 57 tests, 0 failures — among them every one of the 6,236 stored
  ayahs compared with the Tanzil file, the 15:49 quarter, 112 basmalas (95:1 and 97:1 with their shadda), 15 sajdahs.
- `bootRun` on the development database: migrations 1.1.0 and 1.1.1 applied; `GET /v1/surahs/112/ayahs` equal to the
  file, UTF-8.
- A class without Javadoc fails Checkstyle (tried with a probe class, then removed).
- `./gradlew bootRun`: Docker Compose started PostgreSQL 18, Flyway applied `1.0.0 - baseline`; `GET /v1/app-info`
  answered in Urdu; an unknown path answered `404 NOT_FOUND_PATH` in French; a repeated request with `If-None-Match`
  answered `304`.
- CI on GitHub: run #2 green (1 min 41 s — build, unit and integration tests, OpenAPI contract unchanged). Run #1
  failed with exit code 126: `gradlew` had been committed from Windows without the execute bit — fixed with
  `git update-index --chmod=+x gradlew`.
