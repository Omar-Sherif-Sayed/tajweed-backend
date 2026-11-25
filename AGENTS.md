# AGENTS.md — tajweed-backend

> **State (2026-11-21): phase 1 started.** Scaffold (`P0-07`) + the **`quran` module**: the Tanzil Uthmani text and
> metadata imported byte for byte after checksum and count checks (6,236 ayahs, 604 pages, 30 juz, 240 quarters with
> the reviewed 15:49 correction), served by `/v1/surahs`, `/v1/pages/{page}`, `/v1/juzs`; the `source` module with
> `GET /v1/content-version`; **Quran search** (F16): Tanzil «Simple» and «Simple (Clean)» imported verbatim as the
> search text, `GET /v1/quran-search`; **the `reciter` module** (2026-11-22): 13 reciters, 37 recitations (27 from
> mp3quran, 10 EveryAyah folders), each riwayah said plainly (non-Hafs = listening only), 8,926 audio files checked,
> 74,977 ayah timings, a daily health check of the sources (ShedLock). 90 tests green. **2026-11-22: the phase 1 contract** — `docs/api/client-guide.md` — and
> **the phones' Quran bundle** (`./gradlew quranBundle`, SQLite written by the import migrations themselves).
> **2026-11-22 (the author: «ابدأ فيها», «اعمله»)**: recitation 38 — EveryAyah's «Hudhaify», identified by the author as
> Ali al-Hudhaifi; **the `radio` module** (133 stations, recitation only, daily stream check); **the `hadith` module** —
> HadeethEnc fetched into the database and refreshed (never in a file); **the `tafsir` module** — the Quran
> Foundation's Content Sync, keys only from the environment; `content_sync`. 97 tests green. Next: the author's keys →
> `./gradlew liveTest`; then the web scaffold (`P0-08`).

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
| ShedLock | 7.11.0 (2026-11-22) | `ext.shedlockVersion` |
| sqlite-jdbc (the `bundle` source set only) | 3.54.1.0 (2026-11-22) | `ext.sqliteJdbcVersion` |
| Runtime image | `eclipse-temurin:25-jre` | `Dockerfile` |
| CI actions | checkout v7, setup-java v6, gradle/actions v6, upload-artifact v7 | `.github/workflows/ci.yml` |

Added 2026-11-22 with its first use: **ShedLock 7.11.0** (`shedlock-spring`, `shedlock-provider-jdbc-template`; checked
on Maven Central that day) for the daily health check of the audio sources. The check itself uses the JDK's
`HttpClient` (one `HEAD` per source), and its test a local JDK `HttpServer` — no WireMock needed yet. 2026-11-22: the
first HTTP interface client (`HadeethEncClient`, `@HttpExchange` on `RestClient`); the Quran Foundation's client uses the
JDK `HttpClient` directly (its token and headers change per call). The tests of both use a local JDK `HttpServer` —
still no WireMock and no new library.

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
                             · GET /v1/quran-search — search/: ArabicSearchNormalizer (the one tidying function),
                             QuranSearchIndex (in memory, built on the first search), SurahNameMatcher (names, references)
    reciter/                 riwayah, reciter, recitation, recitation_surah, audio_source, ayah_timing:
                             GET /v1/reciters, /v1/reciters/{id}, /v1/recitations/{id}, …/surahs/{n}/timings
                             · scheduler/: AudioSourceHealthChecker (one HEAD per source), AudioSourceHealthScheduler
                             (daily, ShedLock), AudioHealthProperties (tajweed.audio-health.*)
    radio/                   radio_station, radio_stream: GET /v1/radios, /v1/radios/{id} · scheduler/:
                             RadioStreamHealthChecker (a GET, first 4 KB, up to three tries), daily with ShedLock
    hadith/                  HadeethEnc: GET /v1/hadith-categories, …/{id}/hadiths, /v1/hadiths/{id} · client/
                             (HTTP interface on RestClient) · scheduler/HadeethEncSync (one request a second, 30 days)
    tafsir/                  GET /v1/tafsirs, /v1/surahs/{n}/ayahs/{a}/tafsirs · client/QuranFoundationClient (OAuth2
                             client credentials, token cache) · scheduler/TafsirSync (Content Sync, 6 days)
    source/                  … and content_sync (ContentSync: the state of every run-time sync), DtoCredit
src/main/java/db/migration/  Java migrations that import content: V1_1_1__ImportQuranText (checksums, counts, insert),
                             V1_2_1__ImportQuranSearchText (the search text, same checks), V1_3_1__ImportRecitations
                             (the reciter catalogue: every choice checked against the sources, every timing file
                             against the Tanzil ayah count)
src/main/resources/
  application.properties     every setting, with its environment variable
  db/migration/              V1_0_0__baseline.sql, V1_1_0__create_content_source_and_quran_tables.sql …
  content/                   source files, byte for byte (-text in .gitattributes): tanzil/quran-uthmani.txt,
                             tanzil/quran-data.xml, tanzil/quran-simple.txt, tanzil/quran-simple-clean.txt,
                             reviewed/hizb-quarter-corrections.csv; mp3quran/ (catalogue, riwayat, 1,482 timing
                             files + SHA256SUMS), everyayah/recitations.js, reviewed/recitations.csv,
                             timing-exclusions.csv, audio-check.csv — docs/data-sources/README.md
  i18n/                      messages.properties (Arabic, default) + messages_<code>.properties × 21
src/bundle/java/…            the phones' Quran bundle (QuranBundleBuilder): a build tool with its own source set,
                             so its SQLite driver never enters the application — ./gradlew quranBundle
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
- **Search** (parent `AGENTS.md` §8.1): the displayed text is never tidied or searched; only the search text is,
  through `ArabicSearchNormalizer` alone — NFC; tatweel and the annotation signs U+06D6 – U+06ED removed;
  أ إ آ ٱ → ا; ى → ي; ة → ه; without tashkeel every mark removed, with tashkeel kept; spaces collapsed. A match is the
  tidied query inside the tidied ayah. `src/test/resources/search/reference-queries.csv` holds ten queries with
  their counts, computed by an independent implementation; the clients will run the same list. Changing a rule
  means changing that file too, with the reason.
- **Recitations** (parent `AGENTS.md` §6.7, §7.3; the author, 2026-11-22): every recitation says its riwayah
  plainly (`DtoRiwayah`: mp3quran's name, the rawi and the qari taken from it, `matchesDisplayedText`); one that is not
  Hafs is `listeningOnly` — played, never followed or highlighted over the Hafs text. Timings only for Hafs. Labels
  and names are the sources' own. An EveryAyah folder is a fallback of an mp3quran recitation only when measured to be
  the same recording (`docs/data-sources/README.md`); otherwise it is its own recitation. `download_allowed` is false
  for every source until its owner allows offline downloads in writing (F28).
- **Run-time state is kept apart from content**: `audio_source_check` (written by the health check) is the one table
  the code writes; the content tables stay `@Immutable`.
- **Content fetched at run time** (HadeethEnc, the Quran Foundation — parent `AGENTS.md` §5): a sync class in the
  module's `scheduler/` writes it with `JdbcTemplate` (`insert … on conflict do update`, one row at a time), its state in
  `content_sync`; the entities stay `@Immutable` for reading. **Nothing they send is ever written into a file of this
  repository** — tests use a local `HttpServer` with placeholder texts marked «TEST ONLY». A broken or incomplete answer
  is refused and the stored content kept. Credentials only from the environment, never logged; `./gradlew liveTest`
  (tag `live`) is the only test that calls the real APIs, by hand, and prints counts only.
- **The phones' Quran bundle** (`src/bundle`, `QuranBundleBuilder`): SQLite tables with the server's column names,
  filled by running `V1_1_1__ImportQuranText` and `V1_2_1__ImportQuranSearchText` on them, then checked against the
  Tanzil files line by line; reproducible (the audit dates are the newest source's date, not the clock). A change to
  those tables means a change here too, and `SCHEMA_VERSION` raised. Part of `./gradlew build`; CI keeps it as the
  artifact `quran-bundle`.
- **The clients' contract** is `docs/api/openapi.yaml` (generated) plus `docs/api/client-guide.md` (how to use it) —
  a change of behaviour updates the guide in the same commit.
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
| `AUDIO_HEALTH_ENABLED`, `AUDIO_HEALTH_CRON`, `AUDIO_HEALTH_TIMEOUT` | the daily `HEAD` check of every audio source (ShedLock: one instance) | `true`, `0 30 3 * * *` (UTC), `20s` |
| `SERVER_FORWARDEDHEADERSSTRATEGY` | `native` behind a reverse proxy (the client's address for the rate limit) | `none` |
| `SWAGGER_UI_ENABLED`, `API_DOCS_ENABLED` | the Swagger page and the OpenAPI document | `true` |
| `HADEETHENC_SYNC_ENABLED`, `HADEETHENC_SYNC_CRON`, `HADEETHENC_SYNC_DELAY`, `HADEETHENC_REFRESH_AFTER`, `HADEETHENC_BASE_URL` | HadeethEnc's sync | `true`, `0 0 2 * * *`, `1s`, `30d`, their API |
| `QF_CLIENT_ID`, `QF_CLIENT_SECRET`, `QF_ENV` | the Quran Foundation's keys — never in a file (`docs/setup/quran-foundation-keys.md`) | —, —, `prelive` |
| `QF_SYNC_ENABLED`, `QF_SYNC_CRON`, `QF_REFRESH_AFTER`, `QF_AUTH_URL`, `QF_API_URL` | the tafsir sync; the two addresses only for tests | `true`, `0 30 2 * * *`, `6d`, their environment's |
| `QF_LIVE_TAFSIRS` | `liveTest` only: tafsir ids to sync instead of the chosen list (pre-live offers none of ours — use `169`) | — |
| (later) `DORAR_API_KEY` | Dorar — never in a file | — |

## Verified on 2026-11-22 (radio, hadith, tafsir)

- `./gradlew build jacocoTestReport integrationTest`: 97 tests, 0 failures — 133 radio stations, Abdul Basit's Warsh
  channel with «ورش عن نافع» and his reciter id, no programme; recitation 38 with its three Hudhaify folders and 114
  surahs; the HadeethEnc sync against a local server (categories, titles, texts as received, an answer without a grade
  refused, Arabic fallback); the tafsir sync against a local server (HTTP Basic token, `x-auth-token` and `x-client-id`,
  bootstrap over two pages, snapshot, row update, a range 1:2 – 1:4 answering for 1:3, the next run from `sync_token`
  with a deletion); the radio check (audio bytes yes; HTML, empty, `500`, closed port no).
- Data checked: 177 mp3quran channels decided; every kept stream answered `200 audio/mpeg` on 2026-11-22 (two that
  answered `404` after eight tries left out).
- **2026-11-26, `./gradlew liveTest` with the author's pre-live keys**: token obtained; Content Sync bootstrap and
  snapshot in the shape the sync reads; **pre-live offers only the tafsirs 159, 169, 170, 381, 804, 926 — none of our
  seven Arabic ones** (they are in the public Quran.com list, so expected in production). The sync now skips a chosen
  tafsir its environment does not offer and records which (`content_sync.message`); the live test checks the mechanism
  on tafsir 169 (`QF_LIVE_TAFSIRS=169`, a throwaway test database only): 293 rows = the 7 + 286 ayahs of surahs 1 – 2,
  1:1 covered. HadeethEnc: 72 languages, 493 categories, hadith 2962 with its grade.

## Verified on 2026-11-22 (the bundle)

- `./gradlew quranBundle`: `tajweed-quran.sqlite` 4,415,488 bytes, the same SHA-256 on two runs; 6,236 ayahs and both
  search texts equal to the Tanzil files; `pragma integrity_check` ok; 2:255 on page 42, juz 3.

## Verified on 2026-11-22 (the reciter module)

- `./gradlew build jacocoTestReport integrationTest`: 90 tests, 0 failures — with the reciters: 13 reciters,
  7 non-Hafs recitations listening only, al-Muaiqli's muallim 38 surahs and al-Minshawi's 1967 recording 110,
  every timing of al-Minshawi's Al-Baqarah equal to mp3quran's file (287 rows), al-Husary's Warsh recitation
  «ورش عن نافع» with rawi «ورش» and qari «نافع», the excluded at-Tawbah answering `404 NOT_FOUND_TIMINGS`; the health
  check against a local HTTP server (200 audio works; HTML, 404, no answer do not; three failures → `FAILING`).
- Data checked before the import: all 8,926 audio files answered `200 audio/mpeg`; 1,482 timing files, of which 119
  surahs fail the ayah count (Ali al-Hudhaifi's 112 of 114 — excluded whole — and 7 others), listed with reasons.

## Verified on 2026-11-21

- `./gradlew build jacocoTestReport integrationTest`: 75 tests, 0 failures — with the search: both search texts
  stored verbatim beside the right ayah (6,236 rows each), the ten reference queries (e.g. «الصبر» 4 ayahs from
  2:45, «الله» 1,842, «مُحَمَّدٌ» with tashkeel 2 from 3:144), «2:255» → ayah 262, «البقرة» → surah 2, the word
  positions for highlighting (112:1) and none guessed where the spellings split words differently (2:104).
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
