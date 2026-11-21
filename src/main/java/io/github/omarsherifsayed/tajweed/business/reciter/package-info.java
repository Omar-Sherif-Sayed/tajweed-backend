/**
 * The reciter module (F3, F4, F11): reciters, their recitations with the riwayah of each, the surahs each one
 * holds, the ordered audio sources the players try, the ayah timings, and the daily health check of the sources.
 * The catalogue is imported by {@code db.migration.V1_3_1__ImportRecitations} from mp3quran.net and EveryAyah and
 * only read here; audio is never copied — the clients stream it from the sources ({@code AGENTS.md} §5).
 */
package io.github.omarsherifsayed.tajweed.business.reciter;
