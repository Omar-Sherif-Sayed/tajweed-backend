package io.github.omarsherifsayed.tajweed.business.quran.service.impl;

import io.github.omarsherifsayed.tajweed.business.quran.mapper.QuranMapper;
import io.github.omarsherifsayed.tajweed.business.quran.repository.AyahRepository;
import io.github.omarsherifsayed.tajweed.business.quran.schema.response.DtoJuz;
import io.github.omarsherifsayed.tajweed.business.quran.service.JuzRetrievalService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads where each juz starts from the imported tables.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JuzRetrievalServiceImpl implements JuzRetrievalService {

    /** Reads the ayahs. */
    private final AyahRepository ayahRepository;

    /**
     * {@inheritDoc}
     *
     * @return where each juz starts, in order
     */
    @Override
    public List<DtoJuz> getJuzs() {
        return ayahRepository.findJuzStarts().stream().map(QuranMapper::toDto).toList();
    }
}
