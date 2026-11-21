-- V1_2_0 — the search text (F16, AGENTS.md §8.1; P1-D3; 2026-11-21).
-- The same 6,236 ayahs in everyday spelling, from Tanzil «Simple» (with tashkeel) and «Simple (Clean)» (without),
-- byte for byte. Used only to search — never displayed. Written by V1_2_1__ImportQuranSearchText after its checks.

CREATE TABLE ayah_search_text (
    ayah_id           integer     CONSTRAINT pk_ayah_search_text PRIMARY KEY
                                  CONSTRAINT fk_ayah_search_text_ayah REFERENCES ayah (pk_id),
    text_simple       text        NOT NULL,
    text_simple_clean text        NOT NULL,
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now()
);
