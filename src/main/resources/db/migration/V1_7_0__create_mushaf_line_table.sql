-- V1_7_0 — the lines of the printed Madinah mushaf, 1405H (F2, AGENTS.md §8.1; P0-12; approved by the author 2026-11-26).
-- 604 pages: 15 lines each (pages 1 and 2: 8). A line is a surah header, the basmala, or a run of the Tanzil text's
-- space-separated words — from (first_ayah_id, first_word) to (last_ayah_id, last_word), words counted from 0 in
-- ayah.text_uthmani. Only positions are stored: the text itself is always Tanzil's (§6.1). Where the lines break comes
-- from the DigitalKhatt old-Madinah layout (MIT); written by V1_7_1__ImportMushafLines after its checks.

CREATE TABLE mushaf_line (
    pk_id         serial      CONSTRAINT pk_mushaf_line PRIMARY KEY,
    page          smallint    NOT NULL CONSTRAINT ck_mushaf_line_page CHECK (page BETWEEN 1 AND 604),
    line          smallint    NOT NULL CONSTRAINT ck_mushaf_line_line CHECK (line BETWEEN 1 AND 15),
    kind          varchar(12) NOT NULL
                              CONSTRAINT ck_mushaf_line_kind CHECK (kind IN ('SURAH_HEADER', 'BASMALA', 'TEXT')),
    surah_number  smallint    CONSTRAINT fk_mushaf_line_surah REFERENCES surah (pk_id),
    first_ayah_id integer     CONSTRAINT fk_mushaf_line_first_ayah REFERENCES ayah (pk_id),
    first_word    smallint    CONSTRAINT ck_mushaf_line_first_word CHECK (first_word >= 0),
    last_ayah_id  integer     CONSTRAINT fk_mushaf_line_last_ayah REFERENCES ayah (pk_id),
    last_word     smallint    CONSTRAINT ck_mushaf_line_last_word CHECK (last_word >= 0),
    created_at    timestamptz NOT NULL DEFAULT now(),
    updated_at    timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_mushaf_line_page_line UNIQUE (page, line),
    -- A surah header or a basmala names its surah and holds no words; a text line holds words and names no surah.
    CONSTRAINT ck_mushaf_line_shape CHECK (
        (kind = 'TEXT' AND surah_number IS NULL AND first_ayah_id IS NOT NULL AND first_word IS NOT NULL
            AND last_ayah_id IS NOT NULL AND last_word IS NOT NULL AND last_ayah_id >= first_ayah_id)
        OR (kind <> 'TEXT' AND surah_number IS NOT NULL AND first_ayah_id IS NULL AND first_word IS NULL
            AND last_ayah_id IS NULL AND last_word IS NULL))
);
