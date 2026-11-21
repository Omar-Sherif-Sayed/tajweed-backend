package io.github.omarsherifsayed.tajweed.business.reciter.schema.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A riwayah, said plainly with every recitation (the author, 2026-11-22): its name, its rawi and its qari, and whether
 * it is the riwayah of the text the application shows.
 *
 * @param id mp3quran's id of the riwayah
 * @param nameAr its name as mp3quran writes it («ورش عن نافع»)
 * @param nameEn its name as mp3quran writes it in English
 * @param rawiAr the rawi («ورش»)
 * @param qariAr the qari («نافع»)
 * @param matchesDisplayedText {@code true} for Hafs an Asim — the riwayah of the displayed text; otherwise listening only
 */
@Schema(description = "A riwayah: its name, rawi and qari, and whether it is the riwayah of the displayed text (Hafs).")
public record DtoRiwayah(int id, String nameAr, String nameEn, String rawiAr, String qariAr, boolean matchesDisplayedText) {
}
