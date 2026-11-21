package io.github.omarsherifsayed.tajweed.business.quran.service.impl;

import io.github.omarsherifsayed.tajweed.business.quran.mapper.QuranMapper;
import io.github.omarsherifsayed.tajweed.business.quran.repository.AyahRepository;
import io.github.omarsherifsayed.tajweed.business.quran.schema.response.DtoPage;
import io.github.omarsherifsayed.tajweed.business.quran.service.PageRetrievalService;
import io.github.omarsherifsayed.tajweed.business.source.service.ContentSourceRetrievalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads the ayahs of a page from the imported tables.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PageRetrievalServiceImpl implements PageRetrievalService {

    /** Reads the ayahs. */
    private final AyahRepository ayahRepository;

    /** Gives the source of the text, shown beside it. */
    private final ContentSourceRetrievalService contentSourceRetrievalService;

    /**
     * {@inheritDoc}
     *
     * @param page the page, 1 to 604
     * @return its ayahs and the source of the text
     */
    @Override
    public DtoPage getPage(int page) {
        return new DtoPage(page,
                ayahRepository.findByPageOrderByIdAsc((short) page).stream().map(QuranMapper::toDto).toList(),
                contentSourceRetrievalService.getSource(ContentSourceRetrievalService.QURAN_TEXT));
    }
}
