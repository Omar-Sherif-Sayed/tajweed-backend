package io.github.omarsherifsayed.tajweed.business.quran.service.impl;

import io.github.omarsherifsayed.tajweed.business.quran.entity.Ayah;
import io.github.omarsherifsayed.tajweed.business.quran.entity.MushafLine;
import io.github.omarsherifsayed.tajweed.business.quran.entity.QfMushafWord;
import io.github.omarsherifsayed.tajweed.business.quran.mapper.QuranMapper;
import io.github.omarsherifsayed.tajweed.business.quran.repository.AyahRepository;
import io.github.omarsherifsayed.tajweed.business.quran.repository.MushafLineRepository;
import io.github.omarsherifsayed.tajweed.business.quran.repository.QfMushafWordRepository;
import io.github.omarsherifsayed.tajweed.business.quran.scheduler.QfMushafSync;
import io.github.omarsherifsayed.tajweed.business.quran.schema.response.DtoPage;
import io.github.omarsherifsayed.tajweed.business.quran.schema.response.DtoQcfPage;
import io.github.omarsherifsayed.tajweed.business.quran.service.PageRetrievalService;
import io.github.omarsherifsayed.tajweed.business.source.service.ContentSourceRetrievalService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    /** The credit the Quran Foundation's font terms ask for. */
    static final String QCF_CREDIT = "Quran fonts provided by Quran Foundation";

    /** Where the credit links. */
    static final String QCF_CREDIT_URL = "https://quran.foundation/";

    /** Reads the ayahs. */
    private final AyahRepository ayahRepository;

    /** Reads the printed lines. */
    private final MushafLineRepository mushafLineRepository;

    /** Reads the QCF V1 glyphs. */
    private final QfMushafWordRepository qfMushafWordRepository;

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
                contentSourceRetrievalService.getSource(ContentSourceRetrievalService.MUSHAF_LAYOUT),
                qcf(page));
    }

    /**
     * The page in QCF V1 glyphs, line by line, with the basmala's glyphs (ayah 1:1 without its number).
     *
     * @param page the page
     * @return the glyphs, or {@code null} while the mushaf is not synced
     */
    private DtoQcfPage qcf(int page) {
        List<QfMushafWord> words = qfMushafWordRepository.findByMushafIdAndPageOrderByPositionAsc(
                QfMushafSync.MUSHAF_ID, (short) page);
        if (words.isEmpty()) {
            return null;
        }
        Map<Integer, List<DtoQcfPage.DtoQcfGlyph>> lines = new LinkedHashMap<>();
        for (QfMushafWord w : words) {
            lines.computeIfAbsent((int) w.getLine(), k -> new ArrayList<>())
                    .add(new DtoQcfPage.DtoQcfGlyph(w.getGlyph(), w.getAyahId(), "end".equals(w.getCharType())));
        }
        StringBuilder basmala = new StringBuilder();
        for (QfMushafWord w : qfMushafWordRepository.findByMushafIdAndAyahIdAndCharTypeOrderByPositionAsc(
                QfMushafSync.MUSHAF_ID, (short) 1, "word")) {
            basmala.append(basmala.isEmpty() ? "" : " ").append(w.getGlyph());
        }
        return new DtoQcfPage(lines.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new DtoQcfPage.DtoQcfLine(e.getKey(), e.getValue())).toList(),
                basmala.toString(), QCF_CREDIT, QCF_CREDIT_URL);
    }
}
