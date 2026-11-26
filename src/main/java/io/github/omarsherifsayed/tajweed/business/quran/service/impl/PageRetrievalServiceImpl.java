package io.github.omarsherifsayed.tajweed.business.quran.service.impl;

import io.github.omarsherifsayed.tajweed.business.quran.entity.MushafLine;
import io.github.omarsherifsayed.tajweed.business.quran.mapper.QuranMapper;
import io.github.omarsherifsayed.tajweed.business.quran.repository.AyahRepository;
import io.github.omarsherifsayed.tajweed.business.quran.repository.MushafLineRepository;
import io.github.omarsherifsayed.tajweed.business.quran.schema.response.DtoPage;
import io.github.omarsherifsayed.tajweed.business.quran.service.PageRetrievalService;
import io.github.omarsherifsayed.tajweed.business.source.service.ContentSourceRetrievalService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads a page from the imported tables: its ayahs and its printed lines.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PageRetrievalServiceImpl implements PageRetrievalService {

    /** Reads the ayahs. */
    private final AyahRepository ayahRepository;

    /** Reads the printed lines. */
    private final MushafLineRepository mushafLineRepository;

    /** Gives the sources of the text and of the line breaks, shown beside them. */
    private final ContentSourceRetrievalService contentSourceRetrievalService;

    /**
     * {@inheritDoc}
     *
     * @param page the page, 1 to 604
     * @return its ayahs, lines and sources
     */
    @Override
    public DtoPage getPage(int page) {
        List<MushafLine> lines = mushafLineRepository.findByPageOrderByLineAsc((short) page);
        return new DtoPage(page,
                ayahRepository.findByPageOrderByIdAsc((short) page).stream().map(QuranMapper::toDto).toList(),
                lines.stream().map(QuranMapper::toDto).toList(),
                contentSourceRetrievalService.getSource(ContentSourceRetrievalService.QURAN_TEXT),
                contentSourceRetrievalService.getSource(ContentSourceRetrievalService.MUSHAF_LAYOUT));
    }
}
