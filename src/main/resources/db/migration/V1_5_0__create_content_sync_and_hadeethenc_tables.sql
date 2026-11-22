-- V1_5_0 — content fetched at run time (2026-11-22): the state of every sync, and HadeethEnc's hadiths (F13, F29, F35).
-- No migration writes their rows: the running backend fetches them from hadeethenc.com/api/v1 and refreshes them
-- (the author's decision of 2026-11-22: HadeethEnc's content lives in the database only, never in a repository).
-- Every column: docs/database/README.md.

-- One row per synced resource (HADEETHENC, QF_TAFSIRS …): when it ran, how it ended, the cursor to continue from.
CREATE TABLE content_sync (
    pk_id            serial       CONSTRAINT pk_content_sync PRIMARY KEY,
    code             varchar(60)  NOT NULL CONSTRAINT uk_content_sync_code UNIQUE,
    status           varchar(10)  NOT NULL CONSTRAINT ck_content_sync_status CHECK (status IN ('RUNNING', 'OK', 'FAILED')),
    last_started_at  timestamptz  NOT NULL,
    last_finished_at timestamptz,
    last_success_at  timestamptz,
    items            integer      NOT NULL DEFAULT 0,
    sync_token       text,
    message          varchar(500),
    created_at       timestamptz  NOT NULL DEFAULT now(),
    updated_at       timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE hadeethenc_category (
    pk_id      integer     CONSTRAINT pk_hadeethenc_category PRIMARY KEY,
    parent_id  integer,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE hadeethenc_category_title (
    pk_id       serial       CONSTRAINT pk_hadeethenc_category_title PRIMARY KEY,
    category_id integer      NOT NULL CONSTRAINT fk_hadeethenc_category_title_category REFERENCES hadeethenc_category (pk_id)
                                      ON DELETE CASCADE,
    language    varchar(5)   NOT NULL,
    title       varchar(500) NOT NULL,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT uk_hadeethenc_category_title UNIQUE (category_id, language)
);

CREATE TABLE hadeethenc_hadith (
    pk_id      integer     CONSTRAINT pk_hadeethenc_hadith PRIMARY KEY,
    listed_at  timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE hadeethenc_hadith_category (
    pk_id       serial      CONSTRAINT pk_hadeethenc_hadith_category PRIMARY KEY,
    hadith_id   integer     NOT NULL CONSTRAINT fk_hadeethenc_hadith_category_hadith REFERENCES hadeethenc_hadith (pk_id)
                                     ON DELETE CASCADE,
    category_id integer     NOT NULL CONSTRAINT fk_hadeethenc_hadith_category_category REFERENCES hadeethenc_category (pk_id)
                                     ON DELETE CASCADE,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_hadeethenc_hadith_category UNIQUE (hadith_id, category_id)
);
CREATE INDEX ix_hadeethenc_hadith_category_category ON hadeethenc_hadith_category (category_id);

-- One row per hadith and language, as received (text exactly as HadeethEnc sends it). hints and words_meanings hold
-- the received JSON arrays as text.
CREATE TABLE hadeethenc_hadith_text (
    pk_id          serial      CONSTRAINT pk_hadeethenc_hadith_text PRIMARY KEY,
    hadith_id      integer     NOT NULL CONSTRAINT fk_hadeethenc_hadith_text_hadith REFERENCES hadeethenc_hadith (pk_id)
                                        ON DELETE CASCADE,
    language       varchar(5)  NOT NULL,
    title          text        NOT NULL,
    hadeeth        text        NOT NULL,
    hadeeth_intro  text,
    attribution    text,
    grade          text        NOT NULL,
    explanation    text,
    hints          text,
    words_meanings text,
    reference      text,
    sha256         varchar(64) NOT NULL,
    fetched_at     timestamptz NOT NULL,
    created_at     timestamptz NOT NULL DEFAULT now(),
    updated_at     timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_hadeethenc_hadith_text UNIQUE (hadith_id, language)
);
