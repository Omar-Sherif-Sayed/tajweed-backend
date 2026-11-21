package io.github.omarsherifsayed.tajweed.business.appinfo.mapper;

import io.github.omarsherifsayed.tajweed.business.appinfo.schema.response.DtoAppInfo;
import io.github.omarsherifsayed.tajweed.business.appinfo.schema.response.DtoAuthor;
import java.util.List;

/**
 * Builds the app-info response — a {@code final} class with static methods, the author's mapper convention
 * ({@code AGENTS.md} §9).
 */
public final class AppInfoMapper {

    /**
     * Not instantiated: static methods only.
     */
    private AppInfoMapper() {
    }

    /**
     * Puts the app-info values together.
     *
     * @param name       the full name in the answer's language
     * @param shortName  the short name in the answer's language
     * @param authorName the author's name in the answer's language
     * @param authorLink the author's link
     * @param apiVersion the API version
     * @param language   the answer's language code
     * @param languages  the codes of all supported languages
     * @return the response object
     */
    public static DtoAppInfo toDto(String name, String shortName, String authorName, String authorLink,
                                   String apiVersion, String language, List<String> languages) {
        return new DtoAppInfo(name, shortName, new DtoAuthor(authorName, authorLink), apiVersion, language,
                List.copyOf(languages));
    }
}
