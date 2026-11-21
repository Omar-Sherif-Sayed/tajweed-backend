package io.github.omarsherifsayed.tajweed.business.quran.service.impl;

import io.github.omarsherifsayed.tajweed.business.quran.entity.Ayah;
import io.github.omarsherifsayed.tajweed.business.quran.entity.AyahSearchText;
import io.github.omarsherifsayed.tajweed.business.quran.mapper.QuranMapper;
import io.github.omarsherifsayed.tajweed.business.quran.repository.AyahRepository;
import io.github.omarsherifsayed.tajweed.business.quran.repository.AyahSearchTextRepository;
import io.github.omarsherifsayed.tajweed.business.quran.schema.response.DtoAyah;
import io.github.omarsherifsayed.tajweed.business.quran.schema.response.DtoQuranSearch;
import io.github.omarsherifsayed.tajweed.business.quran.schema.response.DtoSearchHit;
import io.github.omarsherifsayed.tajweed.business.quran.schema.response.DtoSurah;
import io.github.omarsherifsayed.tajweed.business.quran.schema.response.DtoSurahCount;
import io.github.omarsherifsayed.tajweed.business.quran.search.ArabicSearchNormalizer;
import io.github.omarsherifsayed.tajweed.business.quran.search.QuranSearchIndex;
import io.github.omarsherifsayed.tajweed.business.quran.search.SurahNameMatcher;
import io.github.omarsherifsayed.tajweed.business.quran.service.QuranSearchService;
import io.github.omarsherifsayed.tajweed.business.quran.service.SurahRetrievalService;
import io.github.omarsherifsayed.tajweed.business.source.service.ContentSourceRetrievalService;
import io.github.omarsherifsayed.tajweed.general.exception.BaseException;
import io.github.omarsherifsayed.tajweed.general.exception.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Searches the Quran with an index built once, on the first search, from the imported search text and surah names.
 * Both are read-only content that changes only with a migration (that is, with a new deployment), so the index never
 * goes stale while the application runs.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuranSearchServiceImpl implements QuranSearchService {

    /** The code of the «Simple» source. */
    static final String SIMPLE = "TANZIL_SIMPLE";

    /** The code of the «Simple (Clean)» source. */
    static final String SIMPLE_CLEAN = "TANZIL_SIMPLE_CLEAN";

    /** Reads the search text, once. */
    private final AyahSearchTextRepository ayahSearchTextRepository;

    /** Reads the found ayahs of one page of results. */
    private final AyahRepository ayahRepository;

    /** Gives the surahs, for their names. */
    private final SurahRetrievalService surahRetrievalService;

    /** Gives the sources shown beside the results. */
    private final ContentSourceRetrievalService contentSourceRetrievalService;

    /** Built on first use; then only read. */
    private volatile Indexes indexes;

    /**
     * {@inheritDoc}
     *
     * @param query     what the user typed
     * @param openSurah the surah open in the reader, or {@code null}
     * @param page      the page of word results, from 0
     * @param size      word results per page
     * @return the three kinds of result
     */
    @Override
    public DtoQuranSearch search(String query, Integer openSurah, int page, int size) {
        boolean marks = ArabicSearchNormalizer.hasMarks(query);
        if (ArabicSearchNormalizer.normalize(query, marks).isEmpty()) {
            throw new BaseException(ErrorCode.INVALID_REQUEST);
        }
        Indexes ix = indexes();

        DtoAyah reference = ix.names().reference(query, openSurah)
                .flatMap(r -> ayahRepository.findBySurahIdAndNumberInSurah((short) r[0], (short) r[1]))
                .map(QuranMapper::toDto)
                .orElse(null);
        List<DtoSurah> surahs = ix.names().match(query).stream().map(n -> ix.surahs().get(n - 1)).toList();

        List<Integer> found = ix.words().find(query);
        int from = (int) Math.min((long) page * size, found.size());
        List<Integer> pageIds = found.subList(from, Math.min(from + size, found.size()));
        Map<Integer, Ayah> rows = ayahRepository.findAllById(pageIds).stream()
                .collect(Collectors.toMap(Ayah::getId, Function.identity()));
        List<DtoSearchHit> hits = pageIds.stream()
                .map(rows::get)
                .map(a -> new DtoSearchHit(QuranMapper.toDto(a),
                        ix.words().matchedWords(a.getId(), query, a.getTextUthmani())))
                .toList();

        return new DtoQuranSearch(query, marks, reference, surahs, found.size(), page, size, hits,
                countsBySurah(found, ix.surahs()),
                contentSourceRetrievalService.getSource(ContentSourceRetrievalService.QURAN_TEXT),
                List.of(contentSourceRetrievalService.getSource(SIMPLE),
                        contentSourceRetrievalService.getSource(SIMPLE_CLEAN)));
    }

    /**
     * How many found ayahs are in each surah.
     *
     * @param ids    the found ayah ids, in order
     * @param surahs the 114 surahs, in order
     * @return one entry per surah that has a result, in order
     */
    private static List<DtoSurahCount> countsBySurah(List<Integer> ids, List<DtoSurah> surahs) {
        Map<Integer, Integer> counts = new TreeMap<>();
        int surah = 0;
        int lastAyahId = 0;
        for (int id : ids) {
            while (id > lastAyahId) {
                lastAyahId += surahs.get(surah).ayahCount();
                surah++;
            }
            counts.merge(surah, 1, Integer::sum);
        }
        List<DtoSurahCount> result = new ArrayList<>();
        counts.forEach((s, c) -> result.add(new DtoSurahCount(s, c)));
        return result;
    }

    /**
     * The indexes, built on the first call.
     *
     * @return the word index, the surah names and the surahs
     */
    private Indexes indexes() {
        Indexes ix = indexes;
        if (ix == null) {
            synchronized (this) {
                ix = indexes;
                if (ix == null) {
                    ix = build();
                    indexes = ix;
                }
            }
        }
        return ix;
    }

    /**
     * Builds the indexes from the database.
     *
     * @return the indexes
     */
    private Indexes build() {
        List<AyahSearchText> rows = ayahSearchTextRepository.findAll(Sort.by("ayahId"));
        QuranSearchIndex words = new QuranSearchIndex(rows.stream().map(AyahSearchText::getTextSimple).toList(),
                rows.stream().map(AyahSearchText::getTextSimpleClean).toList());
        List<DtoSurah> surahs = surahRetrievalService.getSurahs();
        SurahNameMatcher names = new SurahNameMatcher(surahs.stream().map(DtoSurah::nameAr).toList(),
                surahs.stream().map(DtoSurah::nameTransliteration).toList(),
                surahs.stream().map(DtoSurah::nameEn).toList());
        return new Indexes(words, names, surahs);
    }

    /**
     * What a search reads from.
     *
     * @param words  the word index
     * @param names  the surah names
     * @param surahs the 114 surahs, in order
     */
    private record Indexes(QuranSearchIndex words, SurahNameMatcher names, List<DtoSurah> surahs) {
    }
}
