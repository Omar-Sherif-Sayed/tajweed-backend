package io.github.omarsherifsayed.tajweed.business.quran.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.github.omarsherifsayed.tajweed.business.quran.repository.AyahRepository;
import io.github.omarsherifsayed.tajweed.business.quran.repository.SurahRepository;
import io.github.omarsherifsayed.tajweed.business.source.service.ContentSourceRetrievalService;
import io.github.omarsherifsayed.tajweed.general.exception.BaseException;
import io.github.omarsherifsayed.tajweed.general.exception.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Proves the surah service's error: asking for an ayah the surah does not have raises {@code NOT_FOUND_AYAH} (the
 * handler turns it into a {@code 404} in the request's language).
 */
@ExtendWith(MockitoExtension.class)
class SurahRetrievalServiceImplTest {

    /** The surahs (not used by this test). */
    @Mock
    private SurahRepository surahRepository;

    /** The ayahs. */
    @Mock
    private AyahRepository ayahRepository;

    /** The sources (not used by this test). */
    @Mock
    private ContentSourceRetrievalService contentSourceRetrievalService;

    /** The service under test. */
    @InjectMocks
    private SurahRetrievalServiceImpl service;

    @Test
    @DisplayName("An ayah the surah does not have raises NOT_FOUND_AYAH")
    void raisesNotFoundForAMissingAyah() {
        // given: Al-Fatihah has no ayah 8
        when(ayahRepository.findBySurahIdAndNumberInSurah((short) 1, (short) 8)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> service.getAyah(1, 8))
                .isInstanceOfSatisfying(BaseException.class,
                        e -> org.assertj.core.api.Assertions.assertThat(e.getErrorCode())
                                .isEqualTo(ErrorCode.NOT_FOUND_AYAH));
    }
}
