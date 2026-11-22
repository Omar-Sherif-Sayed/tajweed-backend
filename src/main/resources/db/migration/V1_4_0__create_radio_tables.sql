-- V1_4_0 — the radio module (F7; phase 3; 2026-11-22, the author: «Recitation only»).
-- Every column and where its value comes from: docs/database/README.md. The content rows are written by the next
-- migration (V1_4_1__ImportRadios) after its checks; radio_stream_check holds run-time state only.

CREATE TABLE radio_station (
    pk_id           smallint     CONSTRAINT pk_radio_station PRIMARY KEY,
    provider        varchar(10)  NOT NULL CONSTRAINT ck_radio_station_provider CHECK (provider IN ('MP3QURAN')),
    provider_key    integer      NOT NULL,
    kind            varchar(10)  NOT NULL CONSTRAINT ck_radio_station_kind CHECK (kind IN ('RECITER', 'RECITERS', 'NATIONAL')),
    reciter_id      smallint     CONSTRAINT fk_radio_station_reciter REFERENCES reciter (pk_id),
    riwayah_id      smallint     CONSTRAINT fk_radio_station_riwayah REFERENCES riwayah (pk_id),
    name_ar         varchar(200) NOT NULL,
    name_en         varchar(200) NOT NULL,
    country_code    varchar(2),
    broadcaster_url varchar(300),
    sort_order      smallint     NOT NULL,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT uk_radio_station_provider_key UNIQUE (provider, provider_key),
    CONSTRAINT uk_radio_station_sort_order UNIQUE (sort_order),
    CONSTRAINT ck_radio_station_country CHECK ((kind = 'NATIONAL') = (country_code IS NOT NULL))
);

CREATE TABLE radio_stream (
    pk_id      serial       CONSTRAINT pk_radio_stream PRIMARY KEY,
    station_id smallint     NOT NULL CONSTRAINT fk_radio_stream_station REFERENCES radio_station (pk_id),
    url        varchar(300) NOT NULL,
    priority   smallint     NOT NULL CONSTRAINT ck_radio_stream_priority CHECK (priority > 0),
    created_at timestamptz  NOT NULL DEFAULT now(),
    updated_at timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT uk_radio_stream_priority UNIQUE (station_id, priority)
);

-- Run-time state, written by the health check (never content).
CREATE TABLE radio_stream_check (
    stream_id            integer     CONSTRAINT pk_radio_stream_check PRIMARY KEY
                                     CONSTRAINT fk_radio_stream_check_stream REFERENCES radio_stream (pk_id),
    status               varchar(10) NOT NULL CONSTRAINT ck_radio_stream_check_status CHECK (status IN ('OK', 'FAILING')),
    consecutive_failures smallint    NOT NULL DEFAULT 0,
    last_checked_at      timestamptz NOT NULL,
    last_ok_at           timestamptz,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now()
);
