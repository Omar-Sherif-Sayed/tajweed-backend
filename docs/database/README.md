# Database — tables, their columns and where every value comes from

PostgreSQL 18. The schema is made only by Flyway migrations (`src/main/resources/db/migration`); Hibernate checks
that the entities match (`ddl-auto=validate`). Conventions (`AGENTS.md` §9): primary key `pk_id`, the audit columns
`created_at` / `updated_at`, every constraint named (`pk_`, `fk_`, `uk_`, `ck_`, index `ix_`), an applied migration is
never edited.

The tables of a new module are written here first, for the author's approval (`AGENTS.md` §1). The `quran` module's
tables were designed on 2026-11-21 after the author's instruction «Start doing everything related to the backend
project»; **no database outside a developer's machine and the tests has them yet**, so any change he asks for is still
a simple edit of these migrations.

## Migrations

| Version | File | What |
| --- | --- | --- |
| 1.0.0 | `V1_0_0__baseline.sql` | the Flyway history starts (creates nothing) |
| 1.1.0 | `V1_1_0__create_content_source_and_quran_tables.sql` | the three tables below |
| 1.1.1 | `db/migration/V1_1_1__ImportQuranText.java` | imports the Quran text and metadata after verifying every file's SHA-256 and every count |
| 1.2.0 | `V1_2_0__create_ayah_search_text_table.sql` | the table `ayah_search_text` |
| 1.2.1 | `db/migration/V1_2_1__ImportQuranSearchText.java` | imports Tanzil «Simple» and «Simple (Clean)» after verifying both SHA-256 and that both have the 6,236 ayahs in the order of `ayah`; records the two files in `content_source` |
| 1.3.0 | `V1_3_0__create_reciter_tables.sql` | the reciter module's tables, `audio_source_check` and `shedlock` |
| 1.3.1 | `db/migration/V1_3_1__ImportRecitations.java` | imports the reciter catalogue and the timings after verifying every file's SHA-256, every choice of the selection against the sources, every audio file's check and every timing file's ayah count |

## `content_source` — every file the content comes from (module `source`)

One row per source file, written by the import that uses it. It feeds `GET /v1/content-version` (the apps refresh
their copies when it changes) and, later, the Sources section (F17).

| Column | Type | Meaning |
| --- | --- | --- |
| `pk_id` | serial | key |
| `code` | varchar(50), unique | stable name, e.g. `TANZIL_UTHMANI` |
| `name` | varchar(200) | the source's own name and version, e.g. "Tanzil Quran Text (Uthmani, Version 1.1)" |
| `resource_path` | varchar(300) | the file inside the application, e.g. `content/tanzil/quran-uthmani.txt` |
| `sha256` | varchar(64) | the file's SHA-256, checked by the import (64 lower-case hex digits — `ck_content_source_sha256`) |
| `licence` | varchar(100) | e.g. "CC BY 3.0 — verbatim copies only" |
| `source_url` | varchar(300) | where it was obtained, e.g. `https://tanzil.net/download/` |
| `obtained_on` | date | the day it was downloaded (or written, for our reviewed tables) |
| `created_at`, `updated_at` | timestamptz | audit |

## `surah` — the 114 surahs (module `quran`)

| Column | Type | Source |
| --- | --- | --- |
| `pk_id` | smallint, 1 – 114 | the surah's number (Tanzil metadata `index`) |
| `name_ar` | varchar(50) | Tanzil metadata `name`, e.g. «الفاتحة» |
| `name_transliteration` | varchar(50) | Tanzil metadata `tname`, e.g. "Al-Faatiha" |
| `name_en` | varchar(100) | Tanzil metadata `ename`, e.g. "The Opening" |
| `revelation_type` | varchar(10): `MECCAN` / `MEDINAN` | Tanzil metadata `type` (☐ `P0-12`: compared with the printed mushaf's index, surah by surah) |
| `revelation_order` | smallint, unique | Tanzil metadata `order` |
| `ayah_count` | smallint | Tanzil metadata `ayas`; the import checks it against the text |
| `created_at`, `updated_at` | timestamptz | audit |

## `ayah` — the 6,236 ayahs (module `quran`)

| Column | Type | Source |
| --- | --- | --- |
| `pk_id` | integer, 1 – 6236 | the ayah's place in the mushaf (Tanzil's order) |
| `surah_id` | smallint → `surah` | the surah (`fk_ayah_surah`) |
| `number_in_surah` | smallint | the ayah's number (unique with the surah — `uk_ayah_surah_number`) |
| `text_uthmani` | text | **the Tanzil Uthmani line, byte for byte** — never edited, normalised or cleaned (§6.1); a test compares all 6,236 with the file |
| `basmala_word_count` | smallint: 0 or 4 | 4 when Tanzil's line begins with the basmala: ayah 1 of every surah except 1 and 9 (Tanzil's own rule). The text keeps the basmala; a client that shows it as a heading leaves out these first 4 words. ⚠ In 95:1 and 97:1 Tanzil writes «بِّسْمِ» with a shadda; it is kept as written (☐ `P0-12`: compared with the printed copy) |
| `page` | smallint, 1 – 604 | Tanzil's Madinah page starts — verified equal to the DigitalKhatt old-Madinah layout on all 604 pages (2026-11-17) |
| `juz` | smallint, 1 – 30 | Tanzil's juz starts |
| `hizb_quarter` | smallint, 1 – 240 | Tanzil's quarter starts, with the reviewed corrections of `content/reviewed/hizb-quarter-corrections.csv` (one today: quarter 106 starts at **15:49**, as printed). Hizb = (quarter − 1) / 4 + 1 |
| `sajdah_type` | varchar(15): `RECOMMENDED` / `OBLIGATORY`, or null | Tanzil metadata `sajda` (15 places), its `type` as given |
| `created_at`, `updated_at` | timestamptz | audit |

Indexes: `ix_ayah_page`, `ix_ayah_juz`, `ix_ayah_hizb_quarter` (reading by page, juz and quarter).

## `ayah_search_text` — the search text (module `quran`, F16, since 1.2.0)

The same ayah in everyday spelling, used **only to search** — never displayed (`AGENTS.md` §8.1). Stored verbatim;
the search tidies it in memory (`ArabicSearchNormalizer`), never in the table.

| Column | Type | Value and source |
| --- | --- | --- |
| `ayah_id` | integer | the ayah (`pk_ayah_search_text`, `fk_ayah_search_text_ayah` → `ayah.pk_id`) |
| `text_simple` | text | Tanzil «Simple» (with tashkeel), byte for byte |
| `text_simple_clean` | text | Tanzil «Simple (Clean)» (without tashkeel), byte for byte |
| `created_at`, `updated_at` | timestamptz | audit |

No index in the database: the search reads the 6,236 rows once and keeps a tidied copy in memory (about 2 MB).

## The `reciter` module (F3, F4, F11, since 1.3.0)

Designed on 2026-11-22 and approved by the author («Yes, reciters next», then his four answers — documentation
repository, `docs/plan/backend-plan.md` §13). Written by `V1_3_1__ImportRecitations` from the sources' answers as
received (`docs/data-sources/README.md`); the code only reads them, except the two run-time tables at the end.

### `riwayah` — the riwayat the recitations use

| Column | Type | Value and source |
| --- | --- | --- |
| `pk_id` | smallint | mp3quran's riwayah id (1 = Hafs an Asim) |
| `name_ar` | varchar(100) | mp3quran's Arabic name, verbatim («ورش عن نافع») |
| `name_en` | varchar(150) | mp3quran's English name, verbatim |
| `rawi_ar` | varchar(50) | the part of the Arabic name before «عن» — the rawi |
| `qari_ar` | varchar(50) | the part after «عن» — the qari |
| `matches_displayed_text` | boolean | true only for Hafs an Asim, the riwayah of the shipped Tanzil text; the others are listening only (`AGENTS.md` §6.7) |
| `created_at`, `updated_at` | timestamptz | audit |

### `reciter`

| Column | Type | Value and source |
| --- | --- | --- |
| `pk_id` | smallint | our id, in the order of the selection |
| `name_ar`, `name_en` | varchar(100) | mp3quran's names (Arabic and English catalogues) |
| `mp3quran_id` | integer | his id on mp3quran (`uk_reciter_mp3quran_id`) |
| `created_at`, `updated_at` | timestamptz | audit |

### `recitation` — one reciter, one riwayah, one style, its surahs and sources

| Column | Type | Value and source |
| --- | --- | --- |
| `pk_id` | smallint | our id, the selection's (`content/reviewed/recitations.csv`) |
| `reciter_id` | smallint | → `reciter` |
| `riwayah_id` | smallint | → `riwayah`; for an mp3quran moshaf its `rewaya_id`, checked against the selection and against the moshaf's label |
| `style` | varchar(10), null | `MURATTAL` / `MUJAWWAD` / `MUALLIM` as the label names it; null when it names none (al-Minshawi's 1967 recording) |
| `name_ar`, `name_en` | varchar(150) | the source's label (mp3quran: «حفص عن عاصم - مرتل»); for an EveryAyah folder, the selection's label |
| `provider` | varchar(10) | `MP3QURAN` (surah files) or `EVERYAYAH` (ayah files) |
| `mp3quran_moshaf_id` | integer, null | mp3quran's moshaf id (`uk_recitation_mp3quran_moshaf_id`) |
| `surah_count` | smallint | how many surahs it holds |
| `has_timings` | boolean | whether ayah timings were imported |
| `created_at`, `updated_at` | timestamptz | audit |

### `recitation_surah` — which surahs a recitation holds

`pk_id` serial · `recitation_id` → `recitation` · `surah_id` → `surah` (`uk_recitation_surah`) · audit. A surah is
listed only when its audio file answered `200` with audio on the check day (`content/reviewed/audio-check.csv`).

### `audio_source` — where the files are, in the order the players try them (F11)

| Column | Type | Value and source |
| --- | --- | --- |
| `pk_id` | serial | |
| `recitation_id` | smallint | → `recitation` |
| `priority` | smallint | 1 first (`uk_audio_source_priority` with the recitation) |
| `kind` | varchar(10) | `SURAH_FILE` / `AYAH_FILE` |
| `provider` | varchar(10) | `MP3QURAN` / `EVERYAYAH` |
| `url_template` | varchar(300) | mp3quran: its `server` + `{surah}.mp3`; EveryAyah: `https://everyayah.com/data/<folder>/{surah}{ayah}.mp3` — `{surah}`, `{ayah}` as three digits |
| `bitrate_kbps` | smallint, null | EveryAyah's stated bitrate; mp3quran states none |
| `download_allowed` | boolean | false for every source until its owner allows offline downloads in writing (F28) |
| `created_at`, `updated_at` | timestamptz | audit |

### `ayah_timing` — mp3quran's ayah timings (Hafs recitations only)

`pk_id` serial · `recitation_id` · `surah_id` · `ayah_number` (0 = what is recited before ayah 1) · `start_ms` ·
`end_ms` (`ck_ayah_timing_order`: end after start; `uk_ayah_timing`) · audit. Only the ayah number and its two times
are taken from each file; mp3quran's page drawings are not. A surah's timings are imported only when they hold every
ayah of the Tanzil count and the times go forward; the ones that do not are listed with their reason in
`content/reviewed/timing-exclusions.csv`, and the import refuses both an unlisted failure and a listed surah that passes.

### Run-time state (never content)

- `audio_source_check` — `audio_source_id` (primary key, → `audio_source`) · `status` (`OK` / `FAILING`) ·
  `consecutive_failures` · `last_checked_at` · `last_ok_at` · audit. Written by the daily health check
  (`AudioSourceHealthChecker`): one `HEAD` of the source's first file; `FAILING` after three failures in a row.
- `shedlock` — ShedLock's own table, in its documented layout (`name`, `lock_until`, `locked_at`, `locked_by`), so
  that a scheduled job runs on one instance only. The one table without `pk_id` and audit columns: its layout is the
  library's.

## Planned (not created yet)

- The radio module (F7), tafsir (F5) and the other modules of `AGENTS.md` §9, each written here first.
