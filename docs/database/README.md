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

## Planned (not created yet)

- The **reciter** module (F3, F4, F11): reciters, recitations (style, riwayah, completeness), audio sources and ayah
  timings — from mp3quran.net (rights clause read 2026-11-20) and `AGENTS.md` §7.3.
