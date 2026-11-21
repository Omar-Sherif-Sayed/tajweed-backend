-- V1_3_0 — the reciter module (F3, F4, F11; P1-D4, P1-B3, P1-B6; 2026-11-22).
-- Every column and where its value comes from: docs/database/README.md. The content rows are written by the next
-- migration (V1_3_1__ImportRecitations) after its checks; audio_source_check and shedlock hold run-time state only.

CREATE TABLE riwayah (
    pk_id                  smallint     CONSTRAINT pk_riwayah PRIMARY KEY,
    name_ar                varchar(100) NOT NULL,
    name_en                varchar(150) NOT NULL,
    rawi_ar                varchar(50)  NOT NULL,
    qari_ar                varchar(50)  NOT NULL,
    matches_displayed_text boolean      NOT NULL,
    created_at             timestamptz  NOT NULL DEFAULT now(),
    updated_at             timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE reciter (
    pk_id       smallint     CONSTRAINT pk_reciter PRIMARY KEY,
    name_ar     varchar(100) NOT NULL,
    name_en     varchar(100) NOT NULL,
    mp3quran_id integer      NOT NULL CONSTRAINT uk_reciter_mp3quran_id UNIQUE,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE recitation (
    pk_id              smallint     CONSTRAINT pk_recitation PRIMARY KEY,
    reciter_id         smallint     NOT NULL CONSTRAINT fk_recitation_reciter REFERENCES reciter (pk_id),
    riwayah_id         smallint     NOT NULL CONSTRAINT fk_recitation_riwayah REFERENCES riwayah (pk_id),
    style              varchar(10)  CONSTRAINT ck_recitation_style CHECK (style IN ('MURATTAL', 'MUJAWWAD', 'MUALLIM')),
    name_ar            varchar(150) NOT NULL,
    name_en            varchar(150) NOT NULL,
    provider           varchar(10)  NOT NULL CONSTRAINT ck_recitation_provider CHECK (provider IN ('MP3QURAN', 'EVERYAYAH')),
    mp3quran_moshaf_id integer      CONSTRAINT uk_recitation_mp3quran_moshaf_id UNIQUE,
    surah_count        smallint     NOT NULL CONSTRAINT ck_recitation_surah_count CHECK (surah_count BETWEEN 1 AND 114),
    has_timings        boolean      NOT NULL,
    created_at         timestamptz  NOT NULL DEFAULT now(),
    updated_at         timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE recitation_surah (
    pk_id         serial      CONSTRAINT pk_recitation_surah PRIMARY KEY,
    recitation_id smallint    NOT NULL CONSTRAINT fk_recitation_surah_recitation REFERENCES recitation (pk_id),
    surah_id      smallint    NOT NULL CONSTRAINT fk_recitation_surah_surah REFERENCES surah (pk_id),
    created_at    timestamptz NOT NULL DEFAULT now(),
    updated_at    timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_recitation_surah UNIQUE (recitation_id, surah_id)
);

CREATE TABLE audio_source (
    pk_id            serial       CONSTRAINT pk_audio_source PRIMARY KEY,
    recitation_id    smallint     NOT NULL CONSTRAINT fk_audio_source_recitation REFERENCES recitation (pk_id),
    priority         smallint     NOT NULL CONSTRAINT ck_audio_source_priority CHECK (priority > 0),
    kind             varchar(10)  NOT NULL CONSTRAINT ck_audio_source_kind CHECK (kind IN ('SURAH_FILE', 'AYAH_FILE')),
    provider         varchar(10)  NOT NULL CONSTRAINT ck_audio_source_provider CHECK (provider IN ('MP3QURAN', 'EVERYAYAH')),
    url_template     varchar(300) NOT NULL,
    bitrate_kbps     smallint     CONSTRAINT ck_audio_source_bitrate CHECK (bitrate_kbps > 0),
    download_allowed boolean      NOT NULL,
    created_at       timestamptz  NOT NULL DEFAULT now(),
    updated_at       timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT uk_audio_source_priority UNIQUE (recitation_id, priority)
);

CREATE TABLE ayah_timing (
    pk_id         serial      CONSTRAINT pk_ayah_timing PRIMARY KEY,
    recitation_id smallint    NOT NULL CONSTRAINT fk_ayah_timing_recitation REFERENCES recitation (pk_id),
    surah_id      smallint    NOT NULL CONSTRAINT fk_ayah_timing_surah REFERENCES surah (pk_id),
    ayah_number   smallint    NOT NULL CONSTRAINT ck_ayah_timing_ayah_number CHECK (ayah_number >= 0),
    start_ms      integer     NOT NULL CONSTRAINT ck_ayah_timing_start CHECK (start_ms >= 0),
    end_ms        integer     NOT NULL,
    created_at    timestamptz NOT NULL DEFAULT now(),
    updated_at    timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_ayah_timing UNIQUE (recitation_id, surah_id, ayah_number),
    CONSTRAINT ck_ayah_timing_order CHECK (end_ms > start_ms)
);

-- Run-time state, written by the health check (never content).
CREATE TABLE audio_source_check (
    audio_source_id      integer     CONSTRAINT pk_audio_source_check PRIMARY KEY
                                     CONSTRAINT fk_audio_source_check_source REFERENCES audio_source (pk_id),
    status               varchar(10) NOT NULL CONSTRAINT ck_audio_source_check_status CHECK (status IN ('OK', 'FAILING')),
    consecutive_failures smallint    NOT NULL DEFAULT 0,
    last_checked_at      timestamptz NOT NULL,
    last_ok_at           timestamptz,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now()
);

-- ShedLock's table (its documented layout), so that a scheduled job runs on one instance only.
CREATE TABLE shedlock (
    name       varchar(64)  NOT NULL CONSTRAINT pk_shedlock PRIMARY KEY,
    lock_until timestamptz  NOT NULL,
    locked_at  timestamptz  NOT NULL,
    locked_by  varchar(255) NOT NULL
);
