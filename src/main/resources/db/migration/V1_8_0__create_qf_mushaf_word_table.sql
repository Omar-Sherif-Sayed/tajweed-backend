-- V1_8_0 — the printed letter shapes of the 1405H mushaf (the author, 2026-11-27): the words of the Quran Foundation's
-- mushaf 2, «QCF V1 — King Fahd Complex Mushaf 1405 hijri print», each as the glyph of its page's QCF V1 font, with
-- its page, line and order. No migration writes these rows: the running backend fills them through the Quran
-- Foundation's Content Sync (resource mushafs:2) — their terms: Content Sync is the only permitted path for an offline
-- copy, synced at least every 7 days; their content is never committed to a repository. The text shown, copied and
-- searched stays Tanzil's (§6.1); a glyph only draws it. Every column: docs/database/README.md.

CREATE TABLE qf_mushaf_word (
    pk_id      serial      CONSTRAINT pk_qf_mushaf_word PRIMARY KEY,
    mushaf_id  smallint    NOT NULL,
    qf_word_id integer     NOT NULL,
    ayah_id    smallint    NOT NULL CONSTRAINT fk_qf_mushaf_word_ayah REFERENCES ayah (pk_id),
    position   smallint    NOT NULL CONSTRAINT ck_qf_mushaf_word_position CHECK (position >= 1),
    char_type  varchar(4)  NOT NULL CONSTRAINT ck_qf_mushaf_word_char_type CHECK (char_type IN ('word', 'end')),
    page       smallint    NOT NULL CONSTRAINT ck_qf_mushaf_word_page CHECK (page BETWEEN 1 AND 604),
    line       smallint    NOT NULL CONSTRAINT ck_qf_mushaf_word_line CHECK (line BETWEEN 1 AND 15),
    glyph      varchar(8)  NOT NULL,
    synced_at  timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_qf_mushaf_word UNIQUE (mushaf_id, qf_word_id)
);
CREATE INDEX ix_qf_mushaf_word_page ON qf_mushaf_word (mushaf_id, page, position);
