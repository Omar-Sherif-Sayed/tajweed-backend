package io.github.omarsherifsayed.tajweed.business.quran.service.impl;

import io.github.omarsherifsayed.tajweed.business.quran.entity.Surah;
import io.github.omarsherifsayed.tajweed.business.quran.mapper.QuranMapper;
import io.github.omarsherifsayed.tajweed.business.quran.repository.AyahRepository;
import io.github.omarsherifsayed.tajweed.business.quran.repository.SurahRepository;
import io.github.omarsherifsayed.tajweed.business.quran.repository.SurahStartProjection;
import io.github.omarsherifsayed.tajweed.business.quran.schema.response.DtoAyah;
import io.github.omarsherifsayed.tajweed.business.quran.schema.response.DtoSurah;
import io.github.omarsherifsayed.tajweed.business.quran.schema.response.DtoSurahAyahs;
import io.github.omarsherifsayed.tajweed.business.quran.service.SurahRetrievalService;
import io.github.omarsherifsayed.tajweed.business.source.service.ContentSourceRetrievalService;
import io.github.omarsherifsayed.tajweed.general.exception.BaseException;
import io.github.omarsherifsayed.tajweed.general.exception.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads the surahs and their ayahs from the imported tables.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SurahRetrievalServiceImpl implements SurahRetrievalService {

    /** Reads the surahs. */
    private final SurahRepository surahRepository;

    /** Reads the ayahs. */
    private final AyahRepository ayahRepository;

    /** Gives the source of the text, shown beside it. */
    private final ContentSourceRetrievalService contentSourceRetrievalService;

    /**
     * {@inheritDoc}
     *
     * @return every surah in order, with the page where it starts
     */
    @Override
    public List<DtoSurah> getSurahs() {
        Map<Short, Short> startPages = ayahRepository.findSurahStarts().stream()
                .collect(Collectors.toMap(SurahStartProjection::getSurahNumber, SurahStartProjection::getStartPage));
        return surahRepository.findAllByOrderByIdAsc().stream()
                .map(s -> QuranMapper.toDto(s, startPages.get(s.getId())))
                .toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param surahNumber the surah, 1 to 114
     * @return the surah, its ayahs and the source of the text
     */
    @Override
    public DtoSurahAyahs getSurahAyahs(int surahNumber) {
        Surah surah = surahRepository.findById((short) surahNumber)
                .orElseThrow(() -> new IllegalStateException("surah not imported: " + surahNumber));
        List<DtoAyah> ayahs = ayahRepository.findBySurahIdOrderByNumberInSurahAsc((short) surahNumber).stream()
                .map(QuranMapper::toDto)
                .toList();
        return new DtoSurahAyahs(QuranMapper.toDto(surah, ayahs.getFirst().page()), ayahs,
                contentSourceRetrievalService.getSource(ContentSourceRetrievalService.QURAN_TEXT));
    }

    /**
     * {@inheritDoc}
     *
     * @param surahNumber the surah, 1 to 114
     * @param ayahNumber  the ayah's number in it
     * @return the ayah
     */
    @Override
    public DtoAyah getAyah(int surahNumber, int ayahNumber) {
        return ayahRepository.findBySurahIdAndNumberInSurah((short) surahNumber, (short) ayahNumber)
                .map(QuranMapper::toDto)
                .orElseThrow(() -> new BaseException(ErrorCode.NOT_FOUND_AYAH));
    }
}
