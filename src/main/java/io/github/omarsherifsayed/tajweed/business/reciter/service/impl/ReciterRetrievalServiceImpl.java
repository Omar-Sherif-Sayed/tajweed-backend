package io.github.omarsherifsayed.tajweed.business.reciter.service.impl;

import io.github.omarsherifsayed.tajweed.business.reciter.entity.AudioSource;
import io.github.omarsherifsayed.tajweed.business.reciter.entity.AudioSourceCheck;
import io.github.omarsherifsayed.tajweed.business.reciter.entity.Recitation;
import io.github.omarsherifsayed.tajweed.business.reciter.entity.Reciter;
import io.github.omarsherifsayed.tajweed.business.reciter.entity.Riwayah;
import io.github.omarsherifsayed.tajweed.business.reciter.enums.AudioProvider;
import io.github.omarsherifsayed.tajweed.business.reciter.mapper.ReciterMapper;
import io.github.omarsherifsayed.tajweed.business.reciter.repository.AudioSourceCheckRepository;
import io.github.omarsherifsayed.tajweed.business.reciter.repository.AudioSourceRepository;
import io.github.omarsherifsayed.tajweed.business.reciter.repository.AyahTimingRepository;
import io.github.omarsherifsayed.tajweed.business.reciter.repository.RecitationRepository;
import io.github.omarsherifsayed.tajweed.business.reciter.repository.RecitationSurahRepository;
import io.github.omarsherifsayed.tajweed.business.reciter.repository.ReciterRepository;
import io.github.omarsherifsayed.tajweed.business.reciter.repository.RiwayahRepository;
import io.github.omarsherifsayed.tajweed.business.reciter.schema.response.DtoAyahTiming;
import io.github.omarsherifsayed.tajweed.business.reciter.schema.response.DtoRecitation;
import io.github.omarsherifsayed.tajweed.business.reciter.schema.response.DtoRecitationSummary;
import io.github.omarsherifsayed.tajweed.business.reciter.schema.response.DtoReciter;
import io.github.omarsherifsayed.tajweed.business.reciter.schema.response.DtoSurahTimings;
import io.github.omarsherifsayed.tajweed.business.reciter.service.ReciterRetrievalService;
import io.github.omarsherifsayed.tajweed.business.source.schema.response.DtoContentSource;
import io.github.omarsherifsayed.tajweed.business.source.service.ContentSourceRetrievalService;
import io.github.omarsherifsayed.tajweed.general.exception.BaseException;
import io.github.omarsherifsayed.tajweed.general.exception.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads the imported reciter catalogue and the health of its sources.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReciterRetrievalServiceImpl implements ReciterRetrievalService {

    /** The code of the mp3quran catalogue's source row. */
    static final String MP3QURAN = "MP3QURAN_CATALOGUE";

    /** The code of the mp3quran timings' source row. */
    static final String TIMINGS = "MP3QURAN_TIMINGS";

    /** The code of the EveryAyah list's source row. */
    static final String EVERYAYAH = "EVERYAYAH_LIST";

    /** Reads the reciters. */
    private final ReciterRepository reciterRepository;

    /** Reads the recitations. */
    private final RecitationRepository recitationRepository;

    /** Reads the riwayat. */
    private final RiwayahRepository riwayahRepository;

    /** Reads which surahs a recitation holds. */
    private final RecitationSurahRepository recitationSurahRepository;

    /** Reads the audio sources. */
    private final AudioSourceRepository audioSourceRepository;

    /** Reads the sources' health. */
    private final AudioSourceCheckRepository audioSourceCheckRepository;

    /** Reads the timings. */
    private final AyahTimingRepository ayahTimingRepository;

    /** Gives the credits. */
    private final ContentSourceRetrievalService contentSourceRetrievalService;

    /**
     * {@inheritDoc}
     *
     * @return the reciters, in the order of the catalogue
     */
    @Override
    public List<DtoReciter> getReciters() {
        Map<Short, Riwayah> riwayat = riwayat();
        Map<Short, List<Recitation>> byReciter = recitationRepository.findAllByOrderByIdAsc().stream()
                .collect(Collectors.groupingBy(Recitation::getReciterId));
        return reciterRepository.findAllByOrderByIdAsc().stream()
                .map(r -> toDto(r, byReciter.getOrDefault(r.getId(), List.of()), riwayat))
                .toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param reciterId the reciter
     * @return the reciter
     */
    @Override
    public DtoReciter getReciter(int reciterId) {
        Reciter reciter = findReciter(reciterId);
        return toDto(reciter, recitationRepository.findByReciterIdOrderByIdAsc(reciter.getId()), riwayat());
    }

    /**
     * {@inheritDoc}
     *
     * @param recitationId the recitation
     * @return the recitation
     */
    @Override
    public DtoRecitation getRecitation(int recitationId) {
        Recitation recitation = findRecitation(recitationId);
        Reciter reciter = reciterRepository.findById(recitation.getReciterId())
                .orElseThrow(() -> new IllegalStateException("reciter not imported: " + recitation.getReciterId()));
        Riwayah riwayah = riwayat().get(recitation.getRiwayahId());
        List<Integer> surahs = recitationSurahRepository.findByRecitationIdOrderBySurahIdAsc(recitation.getId()).stream()
                .map(s -> (int) s.getSurahId())
                .toList();
        List<AudioSource> sources = audioSourceRepository.findByRecitationIdOrderByPriorityAsc(recitation.getId());
        Map<Integer, AudioSourceCheck> checks = audioSourceCheckRepository
                .findAllById(sources.stream().map(AudioSource::getId).toList()).stream()
                .collect(Collectors.toMap(AudioSourceCheck::getId, Function.identity()));
        List<DtoContentSource> credits = new ArrayList<>();
        credits.add(contentSourceRetrievalService.getSource(MP3QURAN));
        if (sources.stream().anyMatch(s -> s.getProvider() == AudioProvider.EVERYAYAH)) {
            credits.add(contentSourceRetrievalService.getSource(EVERYAYAH));
        }
        if (recitation.getHasTimings()) {
            credits.add(contentSourceRetrievalService.getSource(TIMINGS));
        }
        return new DtoRecitation(ReciterMapper.toDto(recitation, riwayah), reciter.getNameAr(), reciter.getNameEn(),
                surahs, sources.stream().map(s -> ReciterMapper.toDto(s, checks.get(s.getId()))).toList(), credits);
    }

    /**
     * {@inheritDoc}
     *
     * @param recitationId the recitation
     * @param surahNumber  the surah
     * @return the timings
     */
    @Override
    public DtoSurahTimings getTimings(int recitationId, int surahNumber) {
        Recitation recitation = findRecitation(recitationId);
        List<DtoAyahTiming> timings = ayahTimingRepository
                .findByRecitationIdAndSurahIdOrderByAyahNumberAsc(recitation.getId(), (short) surahNumber).stream()
                .map(ReciterMapper::toDto)
                .toList();
        if (timings.isEmpty()) {
            throw new BaseException(ErrorCode.NOT_FOUND_TIMINGS);
        }
        return new DtoSurahTimings(recitationId, surahNumber, timings, contentSourceRetrievalService.getSource(TIMINGS));
    }

    /**
     * A reciter with his recitations, Hafs first, each in the order of the catalogue.
     *
     * @param reciter     the reciter
     * @param recitations his recitations
     * @param riwayat     every riwayah, by id
     * @return the response object
     */
    private static DtoReciter toDto(Reciter reciter, List<Recitation> recitations, Map<Short, Riwayah> riwayat) {
        List<DtoRecitationSummary> list = recitations.stream()
                .map(r -> ReciterMapper.toDto(r, riwayat.get(r.getRiwayahId())))
                .sorted((a, b) -> Boolean.compare(a.listeningOnly(), b.listeningOnly()))
                .toList();
        return new DtoReciter(reciter.getId(), reciter.getNameAr(), reciter.getNameEn(), list);
    }

    /**
     * Every riwayah, by id.
     *
     * @return the riwayat
     */
    private Map<Short, Riwayah> riwayat() {
        return riwayahRepository.findAll().stream().collect(Collectors.toMap(Riwayah::getId, Function.identity()));
    }

    /**
     * A reciter, or the error a client gets.
     *
     * @param id the id asked for
     * @return the reciter
     */
    private Reciter findReciter(int id) {
        if (id < 1 || id > Short.MAX_VALUE) {
            throw new BaseException(ErrorCode.NOT_FOUND_RECITER);
        }
        return reciterRepository.findById((short) id).orElseThrow(() -> new BaseException(ErrorCode.NOT_FOUND_RECITER));
    }

    /**
     * A recitation, or the error a client gets.
     *
     * @param id the id asked for
     * @return the recitation
     */
    private Recitation findRecitation(int id) {
        if (id < 1 || id > Short.MAX_VALUE) {
            throw new BaseException(ErrorCode.NOT_FOUND_RECITATION);
        }
        return recitationRepository.findById((short) id)
                .orElseThrow(() -> new BaseException(ErrorCode.NOT_FOUND_RECITATION));
    }
}
