package io.github.omarsherifsayed.tajweed.business.source.mapper;

import io.github.omarsherifsayed.tajweed.business.source.entity.ContentSource;
import io.github.omarsherifsayed.tajweed.business.source.schema.response.DtoContentSource;

/**
 * Builds the source module's response objects ({@code AGENTS.md} §9: {@code final}, static methods).
 */
public final class ContentSourceMapper {

    /**
     * Not instantiated: static methods only.
     */
    private ContentSourceMapper() {
    }

    /**
     * A content source as the clients see it.
     *
     * @param source the database row
     * @return the response object
     */
    public static DtoContentSource toDto(ContentSource source) {
        return new DtoContentSource(source.getCode(), source.getName(), source.getLicence(), source.getSourceUrl(),
                source.getSha256(), source.getObtainedOn());
    }
}
