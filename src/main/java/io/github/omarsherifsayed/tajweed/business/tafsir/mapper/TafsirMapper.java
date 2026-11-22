package io.github.omarsherifsayed.tajweed.business.tafsir.mapper;

import io.github.omarsherifsayed.tajweed.business.tafsir.entity.Tafsir;
import io.github.omarsherifsayed.tajweed.business.tafsir.schema.response.DtoTafsir;
import io.github.omarsherifsayed.tajweed.business.source.schema.response.DtoCredit;

/**
 * Builds the tafsir module's response objects ({@code AGENTS.md} §9: {@code final}, static methods).
 */
public final class TafsirMapper {

    /** The credit the Quran Foundation's terms ask for. */
    public static final DtoCredit QURAN_FOUNDATION = new DtoCredit("Quran data provided by Quran Foundation",
            "https://quran.foundation", "Under the Quran Foundation's Developer Terms; synced through Content Sync.");

    /**
     * Not instantiated: static methods only.
     */
    private TafsirMapper() {
    }

    /**
     * A tafsir.
     *
     * @param tafsir the row
     * @return the response object
     */
    public static DtoTafsir toDto(Tafsir tafsir) {
        return new DtoTafsir(tafsir.getId(), tafsir.getNameAr(), tafsir.getNameEn(), tafsir.getAuthorAr(),
                tafsir.getAuthorEn(), tafsir.getLanguage());
    }
}
