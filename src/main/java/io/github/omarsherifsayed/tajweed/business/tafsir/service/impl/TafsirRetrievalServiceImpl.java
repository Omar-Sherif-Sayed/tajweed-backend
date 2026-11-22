package io.github.omarsherifsayed.tajweed.business.tafsir.service.impl;

import io.github.omarsherifsayed.tajweed.business.tafsir.entity.Tafsir;
import io.github.omarsherifsayed.tajweed.business.tafsir.entity.TafsirText;
import io.github.omarsherifsayed.tajweed.business.tafsir.mapper.TafsirMapper;
import io.github.omarsherifsayed.tajweed.business.tafsir.repository.TafsirRepository;
import io.github.omarsherifsayed.tajweed.business.tafsir.repository.TafsirTextRepository;
import io.github.omarsherifsayed.tajweed.business.tafsir.schema.response.DtoAyahTafsir;
import io.github.omarsherifsayed.tajweed.business.tafsir.schema.response.DtoAyahTafsirs;
import io.github.omarsherifsayed.tajweed.business.tafsir.schema.response.DtoTafsirs;
import io.github.omarsherifsayed.tajweed.business.tafsir.service.TafsirRetrievalService;
import io.github.omarsherifsayed.tajweed.general.exception.BaseException;
import io.github.omarsherifsayed.tajweed.general.exception.ErrorCode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads the synced tafsirs. An ayah's text is the one whose range covers it (a tafsir may explain several ayahs
 * together); a tafsir without text for an ayah is left out of the answer.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TafsirRetrievalServiceImpl implements TafsirRetrievalService {

    /** Reads the tafsirs. */
    private final TafsirRepository tafsirRepository;

    /** Reads their texts. */
    private final TafsirTextRepository tafsirTextRepository;

    /** Reads the ayah ids and keys. */
    private final JdbcTemplate jdbc;

    /**
     * {@inheritDoc}
     *
     * @return the tafsirs
     */
    @Override
    public DtoTafsirs getTafsirs() {
        return new DtoTafsirs(tafsirRepository.findAllByOrderBySortOrderAsc().stream().map(TafsirMapper::toDto).toList(),
                TafsirMapper.QURAN_FOUNDATION);
    }

    /**
     * {@inheritDoc}
     *
     * @param surahNumber the surah
     * @param ayahNumber  the ayah
     * @param tafsirId    one tafsir, or {@code null}
     * @return the texts
     */
    @Override
    public DtoAyahTafsirs getAyahTafsirs(int surahNumber, int ayahNumber, Integer tafsirId) {
        List<Integer> ids = jdbc.queryForList("select pk_id from ayah where surah_id = ? and number_in_surah = ?",
                Integer.class, surahNumber, ayahNumber);
        if (ids.isEmpty()) {
            throw new BaseException(ErrorCode.NOT_FOUND_AYAH);
        }
        if (tafsirId != null && !tafsirRepository.existsById(tafsirId)) {
            throw new BaseException(ErrorCode.NOT_FOUND_TAFSIR);
        }
        Map<Integer, Tafsir> tafsirs = new HashMap<>();
        for (Tafsir t : tafsirRepository.findAllByOrderBySortOrderAsc()) {
            tafsirs.put(t.getId(), t);
        }
        List<TafsirText> texts = new ArrayList<>(tafsirTextRepository.findCovering(ids.getFirst().shortValue()));
        texts.removeIf(t -> !tafsirs.containsKey(t.getTafsirId()) || (tafsirId != null && !tafsirId.equals(t.getTafsirId())));
        texts.sort((a, b) -> tafsirs.get(a.getTafsirId()).getSortOrder() - tafsirs.get(b.getTafsirId()).getSortOrder());
        List<DtoAyahTafsir> list = new ArrayList<>();
        for (TafsirText t : texts) {
            list.add(new DtoAyahTafsir(TafsirMapper.toDto(tafsirs.get(t.getTafsirId())), key(t.getAyahId()),
                    key(t.getAyahToId()), t.getText()));
        }
        return new DtoAyahTafsirs(surahNumber, ayahNumber, list, TafsirMapper.QURAN_FOUNDATION);
    }

    /**
     * The verse key of an ayah id.
     *
     * @param ayahId the ayah
     * @return {@code surah:ayah}
     */
    private String key(short ayahId) {
        return jdbc.queryForObject("select surah_id || ':' || number_in_surah from ayah where pk_id = ?", String.class,
                ayahId);
    }
}
