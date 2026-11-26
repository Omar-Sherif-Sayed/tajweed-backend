# The phase 1 API — a guide for the web, Android and iPhone clients (`P0-14`, 2026-11-22)

The exact contract is [`openapi.yaml`](openapi.yaml), generated from the code — every path, parameter and field. This
guide says **how the clients are to use it**, and the rules that are not visible in a schema. The product's rules are
in the parent `AGENTS.md` (§6 data integrity, §8.1 – §8.3 reader, search and playback); this guide applies them.

## 1. Every call

| | |
| --- | --- |
| Base | `https://api.<domain>/v1/…` (the domain is chosen at release, `RT-01`); locally `http://localhost:8080/v1/…` |
| Method | `GET` only (and `HEAD`, `OPTIONS`). There is no login, no key, no cookie, no user data. |
| Language | `Accept-Language` with one of the 22 codes (`ar`, `en`, `ur`, …; a region is ignored: `zh-CN` → `zh`); anything else answers in Arabic. It changes only `message` and the names that exist in that language — never the Quran text. |
| Envelope | Every answer, success or error: `{ "success": true, "code": "OK", "message": "…", "data": … }`. An error has `success: false`, a `code` from the list below and `data: null`. |
| Caching | Every answer carries an `ETag`; send it back as `If-None-Match` and a `304` (no body) means "unchanged". `Cache-Control` says how long to keep it: content 1 day, a recitation with its sources' health 1 hour, app info 1 hour. |
| Limit | 120 requests a minute per client by default. Above it: `429 RATE_LIMITED` with `Retry-After` (seconds) — wait, then retry. |
| Browser | CORS allows the web application's own origin only. |

Error codes (a client shows `message`, or its own text for the code — codes never change name):

| Code | Status | When |
| --- | --- | --- |
| `NOT_FOUND_PATH` | 404 | no such path |
| `NOT_FOUND_AYAH` | 404 | the surah has no ayah with this number |
| `NOT_FOUND_RECITER` · `NOT_FOUND_RECITATION` | 404 | unknown id |
| `NOT_FOUND_TIMINGS` | 404 | the recitation has no ayah timings for this surah |
| `INVALID_REQUEST` | 400 | a missing or invalid parameter (a surah outside 1 – 114, a query too long …) |
| `METHOD_NOT_ALLOWED` | 405 | not `GET` |
| `RATE_LIMITED` | 429 | too many requests — see `Retry-After` |
| `INTERNAL_ERROR` | 500 | our fault; retry later |

## 2. Content version and the phones' offline copy

- `GET /v1/content-version` → `version` (16 hex digits) and every source with its SHA-256. A client keeps the version
  of what it has cached; when the server's version differs, it refreshes its cache.
- **The phones do not download the Quran at first launch**: they ship `tajweed-quran.sqlite` inside the application
  (built by this repository, `./gradlew quranBundle` → `build/bundle/`, with `tajweed-quran.manifest.json`: its
  SHA-256, `schemaVersion`, `contentVersion`). Its tables are the server's (`surah`, `ayah`, `ayah_search_text`,
  `content_source`) with the same column names (`docs/database/README.md`), plus `bundle_info`
  (`schema_version`, `content_version`). The app checks the file's SHA-256 against the manifest in a test, and
  compares each of its `content_source.sha256` with the server's entry of the same `code` to know whether its Quran
  text is current. ⚠ The bundle's `content_version` covers only its own five sources; the server's covers every
  source — compare the per-source checksums, not the two versions.
- ☐ How the bundle reaches the phone repositories (a GitHub release of this repository, or a copy committed there) is
  decided on the Android scaffold day (`P0-09`); until then CI keeps it as the build artifact `quran-bundle`.

## 3. The reader (F2) and the index

| Need | Call |
| --- | --- |
| The index of the mushaf (114 surahs: names, Makki / Madani, order of revelation, ayahs, first page) | `GET /v1/surahs` |
| The 30 juz with their first ayah and page | `GET /v1/juzs` |
| A page of the Madinah mushaf (1 – 604) | `GET /v1/pages/{page}` — its ayahs (every page begins with a new ayah, so they all start on it) and its **lines as printed** (1405H) |
| A surah, or one ayah | `GET /v1/surahs/{n}/ayahs` · `GET /v1/surahs/{n}/ayahs/{a}` |

Rules:

- **`text` is shown exactly as received** — never trimmed, re-joined, re-encoded or "cleaned" (§6.1). It is drawn with
  the Quran font the reader chose (the King Fahd page fonts by default, §8.1).
- **The basmala.** When `basmalaWordCount` is 4, the text begins with the basmala: a view that draws the basmala as a
  heading leaves out the first four space-separated words; a view that shows the text as one block shows it whole.
  Al-Fatihah (its basmala is ayah 1) and at-Tawbah (none) have 0.
- `hizb` is derived from `hizbQuarter` (four quarters a hizb). The quarter at 15:49 follows the printed copy (Tanzil
  says 15:50) — already applied.
- Every Quran screen shows **`textSource`** (Tanzil's name and link): Tanzil's licence requires it.
- **The printed page** (since 1.7.0, 2026-11-26): `lines` gives the page's 15 lines (8 on pages 1 and 2), top to
  bottom, each `SURAH_HEADER` (its `surahNumber`; the client adds «ترتيبها» and «آياتها» as the printed copy does),
  `BASMALA` (the client draws the first ayah's own first four words) or `TEXT`: the words from `firstWord` of
  `firstAyahId` to `lastWord` of `lastAyahId`, both included — the ayahs' `text` split at single spaces, counted from 0
  (the basmala's words in front of an ayah 1 are never on a text line). Draw each word exactly as received, the ayah's
  number after its last word, and ۞ before each ayah listed in `rubAyahIds` (the first ayah of a hizb quarter, except
  at an ayah 1 — the printed rule; 15:49 as printed). The line breaks come from the DigitalKhatt old-Madinah layout (MIT, **`layoutSource`**,
  credited with the text's source); only positions are stored, the text is always Tanzil's. Each line is justified to
  the page's width; with a font other than the King Fahd page fonts the letters differ from the print, the words and
  the lines do not.

## 4. Search (F16)

`GET /v1/quran-search?q=…&page=0&size=20[&surah=N]` — one box, three kinds of result in one answer:

| Field | Meaning | What the client does |
| --- | --- | --- |
| `ayah` | the ayah a reference names (`2:255`, `٢:٢٥٥`, «البقرة 255», `255` with `surah=2`) | offer "open 2:255" first |
| `surahs` | the surahs a name names (Arabic with or without «سورة» / «ال», Latin spellings, a number) | list them |
| `hits`, `total`, `countsBySurah` | the ayahs that contain the words, by page, in mushaf order, counted by surah | list, grouped by surah |
| `withTashkeel` | whether the query had marks (then the marks had to agree) | show it as a hint |

- Highlighting is a display layer (§6.3): `matchedWords` are indexes of the space-separated words of `ayah.text`,
  from 0; **an empty list means highlight the whole ayah** (the two spellings split words differently in 363 ayahs).
- The phones search offline in the bundle's `ayah_search_text` with the **same tidying rules** (`AGENTS.md` §8.1
  rule 3), and must pass the same ten reference queries: `src/test/resources/search/reference-queries.csv`.

## 5. Recitation (F3, F4) and resilient playback (F11)

| Need | Call |
| --- | --- |
| The reciter picker | `GET /v1/reciters` — each reciter with his recitations, Hafs first |
| What a player needs | `GET /v1/recitations/{id}` — `surahs`, `sources` in order, `credits` |
| Following the ayahs in a surah file | `GET /v1/recitations/{id}/surahs/{n}/timings` |

Rules:

1. **Say the riwayah plainly** (the author, 2026-11-22): every recitation shows `riwayah.nameAr` (or `nameEn`); when
   `listeningOnly` is true (not Hafs), the screen says «للاستماع فقط — النص المعروض برواية حفص عن عاصم» and the
   player **does not follow or highlight the Hafs text** (§6.7).
2. **Only what exists**: offer a surah only if it is in `surahs` (al-Muaiqli's muallim has 38). A `style` may be
   absent — then show the recitation's own label (`nameAr`).
3. **A file's address**: `urlTemplate` with `{surah}` and `{ayah}` written as three digits (`2:255` → `002`, `255`).
   `SURAH_FILE` sources have `{surah}` only; `AYAH_FILE` sources have both.
4. **Per-ayah playback and highlighting**:
   - with an `AYAH_FILE` source: play the ayah files in order;
   - with a `SURAH_FILE` source: play the surah file and use the timings (milliseconds; ayah 0, when present, is what
     comes before ayah 1); seek to `startMs` to start at an ayah;
   - with neither (`hasTimings` false and no ayah files): whole surahs only — the range modes are not offered.
5. **Resilient playback (§8.3)**: try the sources in `priority` order. On an error, retry the same source with a
   growing delay (capped at 30 s), then the next, round and round, **without limit**, until the user stops; when the
   device's network comes back, retry at once; a file resumes where it stopped; the screen shows «جارٍ إعادة الاتصال…».
   `status: FAILING` only means the daily check failed three times — try it after the others, never drop it.
   A fallback source of the same recitation is the same recording (measured), so the position carries over; but a
   fallback `AYAH_FILE` source resumes at the ayah, not at the millisecond.
6. **Downloads (F28)**: allowed only where `downloadAllowed` is true — today **no source** (waiting for the owners'
   written permission). Otherwise streaming only, and the screen says so.
7. **Credits**: show `credits` (mp3quran, and EveryAyah when one of its folders is a source) on the player.

## 6. Radio (F7)

| Need | Call |
| --- | --- |
| The stations | `GET /v1/radios` — `stations` in order, `credits` (mp3quran) |
| One station | `GET /v1/radios/{id}` |

- Recitation only (the author, 2026-11-22): a station is one reciter (`RECITER`, with `reciterId` when he is in
  `/v1/reciters`) or several (`RECITERS`). `riwayah` is given only when the channel names it; otherwise it is absent and
  the screen does not guess one.
- Play `streams` in `priority` order with the rules of §5 rule 5 (retry, next stream, without limit); a radio resumes at
  the live point. `status: FAILING` = three failed daily checks — try it last, never drop it.
- The stream is played directly from its address; never relayed through our server (`AGENTS.md` §8.4).

## 7. Hadith — HadeethEnc (F13, F29, F35)

| Need | Call |
| --- | --- |
| The categories (a tree: `parentId`) | `GET /v1/hadith-categories` |
| The hadiths of a category and its sub-categories | `GET /v1/hadith-categories/{id}/hadiths?page=0&size=20` |
| One hadith | `GET /v1/hadiths/{id}` |

- A hadith is always given in Arabic (`arabic`); `translation` is the request's language when HadeethEnc publishes it,
  otherwise absent — then the screen shows the Arabic with a note (`AGENTS.md` §6.10). Titles say their language
  (`titleLanguage`).
- **Show everything as received**: the text, `attribution`, `grade` (never apart from the hadith), `explanation`,
  `hints`, `wordsMeanings`, `reference`. Nothing is shortened or reworded — their condition and ours.
- **Credit on every hadith screen**: `credit.name` and a link to `sourceUrl` (the hadith's page on HadeethEnc) — their
  condition of 2026-11-22.
- The phones keep what they have read for offline use (their permission covers it); the content is never written into
  a file of a repository.

## 8. Tafsir (F5, F26)

| Need | Call |
| --- | --- |
| The tafsirs, in order | `GET /v1/tafsirs` |
| Every tafsir of an ayah ("read all its tafsirs" — F26) | `GET /v1/surahs/{n}/ayahs/{a}/tafsirs` |
| One tafsir of an ayah | `GET /v1/tafsirs/{id}/surahs/{n}/ayahs/{a}` |

- A text may cover several ayahs: `from` … `to` (`surah:ayah`). Show it once for the whole range, with the range.
- `text` is HTML as the Quran Foundation sends it: render the markup, never edit the words.
- Show `credit.name` («Quran data provided by Quran Foundation») and each tafsir's name — their terms.
- The list is empty until the backend has synced (it needs the Quran Foundation's credentials); with the pre-live
  credentials only surahs 1 – 2 have texts.

## 9. About (F15) and Sources (F17)

- `GET /v1/app-info` — the name in the request's language (Arabic or English), the author and his one link, the API
  version, the 22 languages.
- `GET /v1/content-version` — every content source with its licence, link, SHA-256 and date: the content part of the
  Sources section. The fonts, the software libraries and the map are credited by each client from its own list.
- The privacy statement waits for the public e-mail and postal address (`RT-04`).

## 10. What is not here yet

Prayer times and the Qibla are calculated on the device (no API); the default method of each country is in the
documentation repository (`docs/data-sources/prayer-methods.md`, `P2-D1`). Adhkar, duas, Dorar's encyclopedias, the
books and the Lives come with their phases. A client never calls mp3quran's, EveryAyah's, HadeethEnc's or the Quran
Foundation's APIs itself — only the audio files and streams, through their addresses.
