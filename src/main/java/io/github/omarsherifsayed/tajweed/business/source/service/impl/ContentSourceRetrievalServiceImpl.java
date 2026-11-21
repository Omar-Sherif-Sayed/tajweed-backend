package io.github.omarsherifsayed.tajweed.business.source.service.impl;

import io.github.omarsherifsayed.tajweed.business.source.entity.ContentSource;
import io.github.omarsherifsayed.tajweed.business.source.mapper.ContentSourceMapper;
import io.github.omarsherifsayed.tajweed.business.source.repository.ContentSourceRepository;
import io.github.omarsherifsayed.tajweed.business.source.schema.response.DtoContentSource;
import io.github.omarsherifsayed.tajweed.business.source.schema.response.DtoContentVersion;
import io.github.omarsherifsayed.tajweed.business.source.service.ContentSourceRetrievalService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads the content sources; the content version is a fingerprint of their checksums.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentSourceRetrievalServiceImpl implements ContentSourceRetrievalService {

    /** Hexadecimal digits kept from the fingerprint: short to show, long enough never to repeat in practice. */
    private static final int VERSION_LENGTH = 16;

    /** Reads the sources. */
    private final ContentSourceRepository contentSourceRepository;

    /**
     * {@inheritDoc}
     *
     * @return the fingerprint of all sources, and the sources
     */
    @Override
    public DtoContentVersion getContentVersion() {
        List<ContentSource> sources = contentSourceRepository.findAllByOrderByIdAsc();
        StringBuilder all = new StringBuilder();
        sources.forEach(s -> all.append(s.getCode()).append('=').append(s.getSha256()).append('\n'));
        String version = sha256(all.toString()).substring(0, VERSION_LENGTH);
        return new DtoContentVersion(version, sources.stream().map(ContentSourceMapper::toDto).toList());
    }

    /**
     * {@inheritDoc}
     *
     * @param code the source's stable code
     * @return the source
     */
    @Override
    public DtoContentSource getSource(String code) {
        return contentSourceRepository.findByCode(code)
                .map(ContentSourceMapper::toDto)
                .orElseThrow(() -> new IllegalStateException("content source not imported: " + code));
    }

    /**
     * The SHA-256 of a text.
     *
     * @param text the text
     * @return 64 lower-case hexadecimal digits
     */
    private static String sha256(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
