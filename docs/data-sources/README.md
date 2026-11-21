# Content source files of this repository

Every file the backend imports content from, with its origin, licence and SHA-256. The files live in
`src/main/resources/content/` (so they ship inside the application and the import can read them), are **never edited**
(`AGENTS.md` §6.1 of the documentation repository), and `.gitattributes` marks them `-text` so Git never changes a
byte. The imports (`db.migration.V1_1_1__ImportQuranText`, `V1_2_1__ImportQuranSearchText`, `V1_3_1__ImportRecitations`) refuse any file whose checksum differs; the same
checksums are recorded in the `content_source` table and served by `GET /v1/content-version`.

The product-wide register of every source (including those used only by the clients) is in the documentation
repository: `docs/data-sources/` and `docs/permissions/README.md`.

| File | Bytes | SHA-256 | Origin | Licence | Checks |
| --- | ---: | --- | --- | --- | --- |
| `content/tanzil/quran-uthmani.txt` | 1,396,087 | `6933e133dd56db778c801bf738848454e43648105a151e8d84d86a7cae39ec5f` | Tanzil Quran Text (Uthmani, Version 1.1), downloaded 2026-11-17 with the author's permission from `https://tanzil.net/pub/download/index.php?marks=true&sajdah=true&tatweel=true&quranType=uthmani&outType=txt-2&agree=true` (options: pause marks, sajdah signs, tatweel below superscript alefs on; rub-el-hizb signs off) | CC BY 3.0 — verbatim copies only; the source shown with a link to tanzil.net; the notice kept in the file (it is, at the end) | 6,236 lines, 114 surahs; the DigitalKhatt old-Madinah layout has the same words in every ayah (2026-11-17); a test compares every stored ayah with this file |
| `content/tanzil/quran-data.xml` | 77,234 | `8867c1d88191472adec9db694b3cd9f135b1a2ef580574d32cf888dcb22c5c7a` | Tanzil Quran Metadata 1.0 — `https://tanzil.net/res/text/metadata/quran-data.xml`, downloaded 2026-11-17 | CC BY 3.0 (Tanzil terms) | 114 surahs, 604 pages (equal to the layout's page starts), 30 juz, 240 quarters, 15 sajdah places |
| `content/reviewed/hizb-quarter-corrections.csv` | 1058 | `842987b2eba1ae152f176512599cc89796ef04a191e8f3b3ad4750baa5c2c138` | Written in this project on 2026-11-21 from the checks of 2026-11-17: one row — quarter 106 starts at 15:49 (Tanzil: 15:50), with its evidence | MIT (this repository) | the import refuses a row whose Tanzil start does not match the metadata |
| `content/tanzil/quran-simple.txt` | 1,353,702 | `b3638f5cf03e2b05e4000efc0e4b656aafb3cf6ed28f8770b6497c5b43c46089` | Tanzil Quran Text (Simple, Version 1.1) — the **search text with tashkeel**, never displayed — downloaded 2026-11-21 with the author's permission («Yes, download both») from `https://tanzil.net/pub/download/index.php?marks=false&sajdah=false&rub=false&tatweel=false&quranType=simple&outType=txt-2&agree=true`. ⚠ Although the options asked for no marks, Tanzil's file still holds the pause marks, ۞ and ۩; the search's tidying function ignores them (U+06D6 – U+06ED), and the file is kept as received | CC BY 3.0 — verbatim copies only | 6,236 lines in the same surah / ayah order as the Uthmani text (checked by the import); a test compares every stored row with this file |
| `content/tanzil/quran-simple-clean.txt` | 794,910 | `1862ee63c70fbfc6285e0bb0b9587b89beb5b7891109a4d543d9d2cf8283e8c8` | Tanzil Quran Text (Simple Clean, Version 1.1) — the **search text without tashkeel**, never displayed — same day, same address with `quranType=simple-clean` | CC BY 3.0 — verbatim copies only | as above; in 363 ayahs the words are split differently from the Uthmani spelling (e.g. «يَـٰٓأَيُّهَا» / «يا أيها») — there the search highlights the whole ayah |

### The reciter catalogue (since 1.3.1, 2026-11-22)

| File | Bytes | SHA-256 | Origin | Licence | Checks |
| --- | ---: | --- | --- | --- | --- |
| `content/mp3quran/reciters-ar.json` | 167,625 | `e883052abb0868fcfdf14a89bb3bcc25a419a1b70fe27e862af3e00d8e5a3a08` | `https://www.mp3quran.net/api/v3/reciters?language=ar`, 2026-11-22 — 244 reciters with their moshafs (id, label, `rewaya_id`, `server`, `surah_list`) | mp3quran.net: «جميع الحقوق متاحة للجميع و يحق لأي زائر أو مطور استخدام اي مادة أو رابط من الموقع» (mp3quran.net/ar/privacy, read 2026-11-20); downloaded with the author's permission (2026-11-22) | the import finds every chosen reciter and moshaf in it, and checks each moshaf's riwayah, style and surah count |
| `content/mp3quran/reciters-en.json` | 165,381 | `1746ff7c10401ba3bce0328e9334696b0b682f45ca996d346af4760505ca5836` | `…/reciters?language=eng`, 2026-11-22 — the English names and labels | mp3quran.net: «جميع الحقوق متاحة للجميع و يحق لأي زائر أو مطور استخدام اي مادة أو رابط من الموقع» (mp3quran.net/ar/privacy, read 2026-11-20); downloaded with the author's permission (2026-11-22) | the same moshafs as the Arabic file |
| `content/mp3quran/riwayat-ar.json` | 2,365 | `729481970579c75439dba2dcef2868f3e2edc285e4288f7b0e9b1226091b01b4` | `…/riwayat?language=ar`, 2026-11-22 — the riwayah names («ورش عن نافع») | mp3quran.net: «جميع الحقوق متاحة للجميع و يحق لأي زائر أو مطور استخدام اي مادة أو رابط من الموقع» (mp3quran.net/ar/privacy, read 2026-11-20); downloaded with the author's permission (2026-11-22) | every chosen riwayah is named «rawi عن qari»; id 1 is «حفص عن عاصم» |
| `content/mp3quran/riwayat-en.json` | 991 | `7632fbd6588b1a81669fd2019a5fe020855c249bcf0fcc03a90ff145bd276277` | `…/riwayat?language=eng`, 2026-11-22 | mp3quran.net: «جميع الحقوق متاحة للجميع و يحق لأي زائر أو مطور استخدام اي مادة أو رابط من الموقع» (mp3quran.net/ar/privacy, read 2026-11-20); downloaded with the author's permission (2026-11-22) | — |
| `content/mp3quran/ayat-timing-reads.json` | 38,306 | `b22204b9af280a4bcfbbf9bfd44129a6744d6b3d97b14fed3306207df8ac7e78` | `…/ayat_timing/reads`, 2026-11-22 — the 115 moshafs with timings | mp3quran.net: «جميع الحقوق متاحة للجميع و يحق لأي زائر أو مطور استخدام اي مادة أو رابط من الموقع» (mp3quran.net/ar/privacy, read 2026-11-20); downloaded with the author's permission (2026-11-22) | timings are imported only for a chosen Hafs moshaf listed here |
| `content/mp3quran/timings/&lt;moshaf&gt;/&lt;surah&gt;.json (1,482 files, 19.9 MB) and `timings/SHA256SUMS`` | 116,166 (manifest) | `65aacbecbb174180b8cf68b46b116c313e4a680833e7db857caa989ddd96d2a2 (manifest)` | `…/ayat_timing?surah=&read=` for the 13 timed Hafs moshafs (112, 118, 119, 53, 51, 106, 133, 92, 54, 31, 81, 30, 74), 2026-11-22; the manifest (ours) holds the SHA-256 of every file | mp3quran.net: «جميع الحقوق متاحة للجميع و يحق لأي زائر أو مطور استخدام اي مادة أو رابط من الموقع» (mp3quran.net/ar/privacy, read 2026-11-20); downloaded with the author's permission (2026-11-22) | every surah: ayahs 1 to the Tanzil count (ayah 0 allowed first), times going forward; failures only through `reviewed/timing-exclusions.csv` — 74,977 rows imported |
| `content/everyayah/recitations.js` | 10,907 | `cde8cc62f7a6d830bd61934d279d8c4ab890d9db440087d1db51a9bd55864ee6` | `https://everyayah.com/data/recitations.js`, 2026-11-22 — EveryAyah's folders with name and bitrate | ⏸ no published terms; request #20971 sent 2026-11-20. The author (2026-11-22): **streaming only** until they answer, credited, downloads off | every chosen folder is in it with its bitrate («64kbps» or «64Kbps») |
| `content/reviewed/recitations.csv` | 5,839 | `76ae598ed5b7176bb556d4f02dc3444c13a71d01a7c2e42f21e9aefb833341e1` | ours, 2026-11-22: the 37 recitations chosen with the author, in order, each with its evidence | MIT (this repository) | the import checks every row against the sources |
| `content/reviewed/timing-exclusions.csv` | 860 | `4a02a15afd7079d320d70d20b6e8c0bf0949f369abaedcc86695616495b1b78b` | ours, 2026-11-22: the mp3quran timings that fail the checks, with the reason — Ali al-Hudhaifi's whole set (112 of 114 surahs miss their last ayah: the numbering cannot be trusted), and 7 single surahs (al-Minshawi, Abdul Basit mujawwad and al-Shuraim at-Tawbah 129 missing; al-Husary Al-Fatihah 7 missing; al-Shuraim Yusuf 5 extra, at-Talaq 1 missing, ash-Shams 1 extra) | MIT (this repository) | the import refuses an unlisted failure, a listed surah that passes, and a whole set that does not fail in most surahs |
| `content/reviewed/audio-check.csv` | 481,761 | `129511595f14866aee2cfd29dfa6124485ec8c87028e6ba9f4352ab94a8cbef0` | ours, 2026-11-22: one `HEAD` of every audio file of the selection — 2,998 mp3quran surah files and the first and last ayah of every surah of 25 EveryAyah folders (5,928) — status, size and content type | MIT (this repository) | all 8,926 answered `200` with an audio content type; a surah is listed for a recitation only when its file did |

**Is an EveryAyah folder the same recording as an mp3quran recitation?** Measured, not assumed: for the last ayah of
every surah, the EveryAyah file's length (bytes × 8 ÷ its stated bitrate) is compared with mp3quran's timing of that
ayah. Same recording when at least 90 % of the ayahs agree within 5 %; the controls (one reciter's files against
another reciter's timings) give 15 – 23 %. Proven the same, and attached as a fallback after the mp3quran file:
al-Minshawi murattal (99 %), al-Husary murattal (100 / 98 %), Abdul Basit murattal (100 %), al-Tablawi murattal
(100 %), al-Shuraim (95 / 94 %). Not the same recording, or not measurable (no timings), so kept as their own
recitations: the other EveryAyah folders of the selection — with Saad al-Ghamdi's folder, named only «Ghamadi»,
matched to him by 75 %. Left out: «Hudhaify» (named only so; Ali al-Hudhaifi's timings cannot be trusted, so the
identity cannot be measured). The script and its results are in the session record of 2026-11-22.

## How a new source file is added

1. The licence is read on the owner's site and recorded in the documentation repository (`docs/permissions/`).
2. The file is downloaded once (with the author's permission), never edited, and placed under `content/<owner>/`.
3. Its SHA-256 is written here, in the import migration (as a constant it checks) and, through the import, in
   `content_source`.
4. The import checks its counts before writing a row; a test compares what was stored with the file.
