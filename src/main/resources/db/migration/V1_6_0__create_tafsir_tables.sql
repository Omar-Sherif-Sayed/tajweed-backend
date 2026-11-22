-- V1_6_0 — the tafsir module (F5, F26; 2026-11-22). No migration writes these rows: the running backend fills them
-- through the Quran Foundation's Content Sync — their terms: «Content Sync is the only permitted path for obtaining
-- and maintaining an offline copy», synced at least every 7 days; their content is never committed to a repository.
-- The tafsirs synced are chosen in content/reviewed/tafsirs.csv. Every column: docs/database/README.md.

CREATE TABLE tafsir (
    pk_id      integer      CONSTRAINT pk_tafsir PRIMARY KEY,
    provider   varchar(20)  NOT NULL CONSTRAINT ck_tafsir_provider CHECK (provider IN ('QURAN_FOUNDATION')),
    name_ar    varchar(300),
    name_en    varchar(300) NOT NULL,
    author_ar  varchar(300),
    author_en  varchar(300),
    language   varchar(50)  NOT NULL,
    sort_order smallint     NOT NULL CONSTRAINT uk_tafsir_sort_order UNIQUE,
    created_at timestamptz  NOT NULL DEFAULT now(),
    updated_at timestamptz  NOT NULL DEFAULT now()
);

-- One row per record of the provider: a text covering one ayah or a range (ayah_id .. ayah_to_id, our ayah ids).
CREATE TABLE tafsir_text (
    pk_id            serial      CONSTRAINT pk_tafsir_text PRIMARY KEY,
    tafsir_id        integer     NOT NULL CONSTRAINT fk_tafsir_text_tafsir REFERENCES tafsir (pk_id) ON DELETE CASCADE,
    source_record_id integer     NOT NULL,
    ayah_id          smallint    NOT NULL CONSTRAINT fk_tafsir_text_ayah REFERENCES ayah (pk_id),
    ayah_to_id       smallint    NOT NULL CONSTRAINT fk_tafsir_text_ayah_to REFERENCES ayah (pk_id),
    text             text        NOT NULL,
    synced_at        timestamptz NOT NULL,
    created_at       timestamptz NOT NULL DEFAULT now(),
    updated_at       timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_tafsir_text_record UNIQUE (tafsir_id, source_record_id),
    CONSTRAINT ck_tafsir_text_range CHECK (ayah_to_id >= ayah_id)
);
CREATE INDEX ix_tafsir_text_ayah ON tafsir_text (ayah_id, ayah_to_id);
