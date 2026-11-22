/**
 * The radio module (F7): Quran radio channels — recitation only (the author, 2026-11-22) — each with its streams in
 * the order the players try them (F11) and a daily health check of every stream. The stations are imported by
 * {@code db.migration.V1_4_1__ImportRadios} from mp3quran.net and only read here; a stream is never relayed through
 * our server — the clients play it from the broadcaster ({@code AGENTS.md} §5, §8.4).
 */
package io.github.omarsherifsayed.tajweed.business.radio;
