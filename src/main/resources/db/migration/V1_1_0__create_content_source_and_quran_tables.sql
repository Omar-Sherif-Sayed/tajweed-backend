-- V1_1_0 — the content_source, surah and ayah tables (phase 1, P1-B1; 2026-11-21).
-- Every column and where its value comes from: docs/database/README.md. The rows are written by the next migration
-- (V1_1_1__ImportQuranText), which verifies every source file's SHA-256 first.

CREATE TABLE content_source (
    pk_id         serial       CONSTRAINT pk_content_source PRIMARY KEY,
    code          varchar(50)  NOT NULL CONSTRAINT uk_content_source_code UNIQUE,
    name          varchar(200) NOT NULL,
    resource_path varchar(300) NOT NULL,
    sha256        varchar(64)  NOT NULL CONSTRAINT ck_content_source_sha256 CHECK (sha256 ~ '^[0-9a-f]{64}$'),
    licence       varchar(100) NOT NULL,
    source_url    varchar(300) NOT NULL,
    obtained_on   date         NOT NULL,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_at    timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE surah (
    pk_id                smallint     CONSTRAINT pk_surah PRIMARY KEY
                                      CONSTRAINT ck_surah_number CHECK (pk_id BETWEEN 1 AND 114),
    name_ar              varchar(50)  NOT NULL,
    name_transliteration varchar(50)  NOT NULL,
    name_en              varchar(100) NOT NULL,
    revelation_type      varchar(10)  NOT NULL
                                      CONSTRAINT ck_surah_revelation_type CHECK (revelation_type IN ('MECCAN', 'MEDINAN')),
    revelation_order     smallint     NOT NULL CONSTRAINT uk_surah_revelation_order UNIQUE,
    ayah_count           smallint     NOT NULL CONSTRAINT ck_surah_ayah_count CHECK (ayah_count > 0),
    created_at           timestamptz  NOT NULL DEFAULT now(),
    updated_at           timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE ayah (
    pk_id              integer     CONSTRAINT pk_ayah PRIMARY KEY
                                   CONSTRAINT ck_ayah_id CHECK (pk_id BETWEEN 1 AND 6236),
    surah_id           smallint    NOT NULL CONSTRAINT fk_ayah_surah REFERENCES surah (pk_id),
    number_in_surah    smallint    NOT NULL CONSTRAINT ck_ayah_number CHECK (number_in_surah > 0),
    text_uthmani       text        NOT NULL,
    basmala_word_count smallint    NOT NULL DEFAULT 0
                                   CONSTRAINT ck_ayah_basmala_word_count CHECK (basmala_word_count IN (0, 4)),
    page               smallint    NOT NULL CONSTRAINT ck_ayah_page CHECK (page BETWEEN 1 AND 604),
    juz                smallint    NOT NULL CONSTRAINT ck_ayah_juz CHECK (juz BETWEEN 1 AND 30),
    hizb_quarter       smallint    NOT NULL CONSTRAINT ck_ayah_hizb_quarter CHECK (hizb_quarter BETWEEN 1 AND 240),
    sajdah_type        varchar(15) CONSTRAINT ck_ayah_sajdah_type CHECK (sajdah_type IN ('RECOMMENDED', 'OBLIGATORY')),
    created_at         timestamptz NOT NULL DEFAULT now(),
    updated_at         timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_ayah_surah_number UNIQUE (surah_id, number_in_surah)
);

CREATE INDEX ix_ayah_page ON ayah (page);
CREATE INDEX ix_ayah_juz ON ayah (juz);
CREATE INDEX ix_ayah_hizb_quarter ON ayah (hizb_quarter);
