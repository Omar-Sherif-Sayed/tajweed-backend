package io.github.omarsherifsayed.tajweed.business.quran.service.impl;

import io.github.omarsherifsayed.tajweed.business.quran.entity.Ayah;
import io.github.omarsherifsayed.tajweed.business.quran.entity.MushafLine;
import io.github.omarsherifsayed.tajweed.business.quran.mapper.QuranMapper;
import io.github.omarsherifsayed.tajweed.business.quran.repository.AyahRepository;
import io.github.omarsherifsayed.tajweed.business.quran.repository.MushafLineRepository;
import io.github.omarsherifsayed.tajweed.business.quran.schema.response.DtoPage;
import io.github.omarsherifsayed.tajweed.business.quran.service.PageRetrievalService;
import io.github.omarsherifsayed.tajweed.business.source.service.ContentSourceRetrievalService;
import java.util.ArrayList;
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
        List<Ayah> ayahs = ayahRepository.findByPageOrderByIdAsc((short) page);
        List<Integer> rubAyahIds = new ArrayList<>();
        for (int i = 0; i < ayahs.size(); i++) {
            Ayah ayah = ayahs.get(i);
            Short before = i > 0 ? ayahs.get(i - 1).getHizbQuarter()
                    : ayahRepository.findById(ayah.getId() - 1).map(Ayah::getHizbQuarter).orElse(null);
            if (ayah.getNumberInSurah() != 1 && before != null && !before.equals(ayah.getHizbQuarter())) {
                rubAyahIds.add(ayah.getId());
            }
        }
        return new DtoPage(page,
                ayahs.stream().map(QuranMapper::toDto).toList(),
                lines.stream().map(QuranMapper::toDto).toList(),
                rubAyahIds,
                contentSourceRetrievalService.getSource(ContentSourceRetrievalService.QURAN_TEXT),
                contentSourceRetrievalService.getSource(ContentSourceRetrievalService.MUSHAF_LAYOUT));
    }
}
