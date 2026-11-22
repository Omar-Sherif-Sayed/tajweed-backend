package io.github.omarsherifsayed.tajweed.business.hadith.mapper;

import io.github.omarsherifsayed.tajweed.business.hadith.entity.HadeethEncHadithText;
import io.github.omarsherifsayed.tajweed.business.hadith.schema.response.DtoHadithText;
import io.github.omarsherifsayed.tajweed.business.hadith.schema.response.DtoWordMeaning;
import io.github.omarsherifsayed.tajweed.business.source.schema.response.DtoCredit;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Builds the hadith module's response objects ({@code AGENTS.md} §9: {@code final}, static methods). The texts are
 * passed on exactly as stored; only the stored JSON arrays are read into lists.
 */
public final class HadithMapper {

    /** HadeethEnc's credit, as their permission of 2026-11-22 asks («العزو للموسوعة كمصدر للمحتوى»). */
    public static final DtoCredit HADEETHENC = new DtoCredit("موسوعة الأحاديث النبوية — HadeethEnc.com",
            "https://hadeethenc.com", "Used with HadeethEnc's written permission of 2026-11-22; not changed.");

    /** Reads the stored arrays. */
    private static final JsonMapper JSON = JsonMapper.builder().build();

    /**
     * Not instantiated: static methods only.
     */
    private HadithMapper() {
    }

    /**
     * A hadith's text.
     *
     * @param text the row
     * @return the response object
     */
    public static DtoHadithText toDto(HadeethEncHadithText text) {
        List<String> hints = new ArrayList<>();
        if (text.getHints() != null) {
            for (JsonNode h : JSON.readTree(text.getHints())) {
                hints.add(h.asString());
            }
        }
        List<DtoWordMeaning> words = new ArrayList<>();
        if (text.getWordsMeanings() != null) {
            for (JsonNode w : JSON.readTree(text.getWordsMeanings())) {
                words.add(new DtoWordMeaning(w.path("word").asString(""), w.path("meaning").asString("")));
            }
        }
        return new DtoHadithText(text.getLanguage(), text.getTitle(), text.getHadeeth(), text.getHadeethIntro(),
                text.getAttribution(), text.getGrade(), text.getExplanation(), hints, words, text.getReference());
    }

    /**
     * A hadith's page on HadeethEnc.
     *
     * @param id       the hadith
     * @param language the language of the page
     * @return its address
     */
    public static String sourceUrl(int id, String language) {
        return "https://hadeethenc.com/" + language + "/browse/hadith/" + id;
    }
}
