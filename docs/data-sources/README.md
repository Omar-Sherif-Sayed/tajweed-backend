# Content source files of this repository

Every file the backend imports content from, with its origin, licence and SHA-256. The files live in
`src/main/resources/content/` (so they ship inside the application and the import can read them), are **never edited**
(`AGENTS.md` §6.1 of the documentation repository), and `.gitattributes` marks them `-text` so Git never changes a
byte. The imports (`db.migration.V1_1_1__ImportQuranText`, `V1_2_1__ImportQuranSearchText`) refuse any file whose checksum differs; the same
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

## How a new source file is added

1. The licence is read on the owner's site and recorded in the documentation repository (`docs/permissions/`).
2. The file is downloaded once (with the author's permission), never edited, and placed under `content/<owner>/`.
3. Its SHA-256 is written here, in the import migration (as a constant it checks) and, through the import, in
   `content_source`.
4. The import checks its counts before writing a row; a test compares what was stored with the file.
