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
| 1.7.0 | `V1_7_0__create_mushaf_line_table.sql` | the table `mushaf_line` (2026-11-26, approved by the author) |
| 1.7.1 | `db/migration/V1_7_1__ImportMushafLines.java` | imports the printed page's lines after verifying the DigitalKhatt layout's SHA-256 and matching every line to the Tanzil words (the checks: `docs/data-sources/README.md`) |

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

### Recitation 38 (1.3.2, 2026-11-22)

`V1_3_2__AddHudhaifyRecitation` adds the rows of `content/reviewed/recitations-added.csv` with the checks of 1.3.1:
EveryAyah's folders named only «Hudhaify» (128, 64 and 32 kbps), which the author identified on 2026-11-22 as Sheikh
Ali al-Hudhaifi — Hafs, its own recitation (not a fallback: the recording cannot be compared with mp3quran's, whose
timings for him fail the ayah count), 114 surahs, three audio sources.

## The radio module (1.4.0 / 1.4.1, 2026-11-22) — F7

The author's answer (2026-11-22): **recitation only**. `V1_4_1__ImportRadios` reads mp3quran's two radio lists and our
decision for every channel (`content/reviewed/radios.csv`); a channel added by mp3quran stops the import until it is
decided. A kept channel must have answered with audio on the check day (`content/reviewed/radio-check.csv`).

### `radio_station`

| Column | Type | Value and source |
| --- | --- | --- |
| `pk_id` | smallint | our id, in mp3quran's order |
| `provider` | varchar(10) | `MP3QURAN` |
| `provider_key` | integer | mp3quran's channel id (`uk_radio_station_provider_key` with the provider) |
| `kind` | varchar(10) | `RECITER` (one reciter), `RECITERS` (several), `NATIONAL` (a country's station — none yet) |
| `reciter_id` | smallint, null | → `reciter`, when the channel is one of our 13 reciters |
| `riwayah_id` | smallint, null | → `riwayah`, only when the channel names it (5 channels: Warsh ×2, Khalaf, al-Susi, Qalun); the missing riwayat were added from mp3quran's list |
| `name_ar`, `name_en` | varchar(200) | mp3quran's names |
| `country_code` | varchar(2), null | a national station's country — required exactly for `NATIONAL` (`ck_radio_station_country`) |
| `broadcaster_url` | varchar(300), null | a national station's own page |
| `sort_order` | smallint | the place in the list (`uk_radio_station_sort_order`) |
| `created_at`, `updated_at` | timestamptz | audit |

### `radio_stream`

`pk_id` (serial) · `station_id` (→ `radio_station`) · `url` (varchar(300), as mp3quran publishes it) · `priority`
(1 first; `uk_radio_stream_priority`) · audit. One stream per station today.

### `radio_stream_check` — run-time state

`stream_id` (primary key, → `radio_stream`) · `status` (`OK` / `FAILING`) · `consecutive_failures` · `last_checked_at` ·
`last_ok_at` · audit. Written by the daily check (`RadioStreamHealthChecker`: a `GET` whose first 4 KB are read, up to
three tries — mp3quran's server answers `500` at times); `FAILING` after three failed days in a row.

## Content fetched at run time (1.5.0, 1.6.0, 2026-11-22)

These tables are filled by the running backend from their owners' APIs — **no migration writes their rows, and none
of their content is in a file of this repository**: HadeethEnc by the author's decision (their permission of
2026-11-22: database only), the Quran Foundation by its terms (Content Sync only).

### `content_sync` — the state of every sync

| Column | Type | Value |
| --- | --- | --- |
| `pk_id` | serial | |
| `code` | varchar(60) | `HADEETHENC`, `QF_TAFSIRS` (`uk_content_sync_code`) |
| `status` | varchar(10) | `RUNNING`, `OK`, `FAILED` |
| `last_started_at`, `last_finished_at`, `last_success_at` | timestamptz | the runs |
| `items` | integer | rows written by the last run |
| `sync_token` | text, null | the Quran Foundation's `next_sync_token`, to continue from |
| `message` | varchar(500), null | an error, in a few words (never a secret) |
| `created_at`, `updated_at` | timestamptz | audit |

### HadeethEnc (F13, F29, F35)

- `hadeethenc_category` — `pk_id` (their id) · `parent_id` (null for a root) · audit.
- `hadeethenc_category_title` — `pk_id` · `category_id` (→, cascade) · `language` · `title` (as received) · audit ·
  `uk_hadeethenc_category_title` (category, language).
- `hadeethenc_hadith` — `pk_id` (their id) · `listed_at` (when last seen in their list) · audit. A hadith no longer
  listed is removed — only when the new list holds at least 90 % of the stored ones.
- `hadeethenc_hadith_category` — `pk_id` · `hadith_id` · `category_id` (both →, cascade) · audit; from the Arabic
  answer's `categories`.
- `hadeethenc_hadith_text` — one row per hadith and language, **exactly as received**: `title`, `hadeeth`,
  `hadeeth_intro`, `attribution`, `grade`, `explanation`, `hints` and `words_meanings` (the received JSON arrays, as
  text), `reference`, `sha256` (of the whole answer), `fetched_at` · audit · `uk_hadeethenc_hadith_text`. An answer
  without its id, title, text or grade is refused and the stored row kept.

Changes from the proposal (`docs/plan/backend-plan.md` §14.2 of the documentation repository): `hints` and
`words_meanings` are `text` holding the received JSON, not `jsonb` (they are only stored and passed on, never queried);
`hadeethenc_hadith` has `listed_at` instead of `fetched_at` (each text has its own `fetched_at`);
`content_sync.last_success_at` added (the schedule looks at the last success, not the last run).

### The tafsirs (F5, F26)

- `tafsir` — `pk_id` (the Quran Foundation's resource id) · `provider` (`QURAN_FOUNDATION`) · `name_ar`, `name_en`,
  `author_ar`, `author_en`, `language` (as their lists give them) · `sort_order` (our choice:
  `content/reviewed/tafsirs.csv`, `uk_tafsir_sort_order`) · audit.
- `tafsir_text` — `pk_id` · `tafsir_id` (→, cascade) · `source_record_id` (their record id; `uk_tafsir_text_record`
  with the tafsir) · `ayah_id`, `ayah_to_id` (→ `ayah`: the range the text covers — their `group_verse_key_from` /
  `group_verse_key_to`, or `verse_key`; `ck_tafsir_text_range`) · `text` (as received, HTML; empty when they have none)
  · `synced_at` · audit · index `ix_tafsir_text_ayah`. An ayah's text is the row whose range covers it.

Change from the proposal: `source_record_id` added (a row change or deletion names the record), and `ayah_to` became
`ayah_to_id` (a foreign key like `ayah_id`).

## `mushaf_line` — the lines of the printed mushaf (module `quran`, F2, since 1.7.0)

The printed Madinah mushaf of 1405H, line by line: 9,046 rows — 604 pages of 15 lines (8 on pages 1 and 2). Only
positions are stored; the text is always `ayah.text_uthmani` (`AGENTS.md` §6.1). Approved by the author on 2026-11-26.

| Column | Type | Value |
| --- | --- | --- |
| `pk_id` | serial | the row |
| `page` | smallint, 1 – 604 | the page |
| `line` | smallint, 1 – 15 | the line; `uk_mushaf_line_page_line` (page, line) |
| `kind` | varchar(12): `SURAH_HEADER` / `BASMALA` / `TEXT` | what the line holds |
| `surah_number` | smallint → `surah` | a header's or a basmala's surah; null on a text line |
| `first_ayah_id`, `first_word` | integer → `ayah`, smallint ≥ 0 | a text line's first word: the index, from 0, of the ayah text's space-separated words |
| `last_ayah_id`, `last_word` | likewise | its last word — the pause marks and the sajdah sign written after it belong to the line |
| `created_at`, `updated_at` | timestamptz | audit |

`ck_mushaf_line_shape`: a text line has its four positions and no surah; a header or a basmala has its surah and no
positions. The basmala's four words in front of an ayah 1 are never on a text line (the `BASMALA` line draws them).

## Planned (not created yet)

- The other modules of `AGENTS.md` §9 (adhkar, dua, library, the Lives, Dorar …), each written here first.
