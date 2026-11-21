package io.github.omarsherifsayed.tajweed.business.quran.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Proves each rule of the tidying function ({@code AGENTS.md} §8.1, rule 3), one by one.
 */
class ArabicSearchNormalizerTest {

    @Test
    @DisplayName("The forms of alef are one letter; ى is ي; ة is ه")
    void foldsLetters() {
        assertThat(ArabicSearchNormalizer.normalize("أإآٱا", false)).isEqualTo("ااااا");
        assertThat(ArabicSearchNormalizer.normalize("هدى", false)).isEqualTo("هدي");
        assertThat(ArabicSearchNormalizer.normalize("سورة", false)).isEqualTo("سوره");
    }

    @Test
    @DisplayName("Without tashkeel every mark goes; with tashkeel the marks stay")
    void removesOrKeepsMarks() {
        assertThat(ArabicSearchNormalizer.normalize("مُحَمَّدٌ", false)).isEqualTo("محمد");
        assertThat(ArabicSearchNormalizer.normalize("مُحَمَّدٌ", true)).isEqualTo("مُحَمَّدٌ");
    }

    @Test
    @DisplayName("Marks typed in another order compare equal (Unicode NFC)")
    void ordersMarks() {
        String shaddaThenFatha = "م" + "ّ" + "َ";
        String fathaThenShadda = "م" + "َ" + "ّ";
        assertThat(ArabicSearchNormalizer.normalize(shaddaThenFatha, true))
                .isEqualTo(ArabicSearchNormalizer.normalize(fathaThenShadda, true));
    }

    @Test
    @DisplayName("Tatweel, pause marks, ۞ and ۩ are not letters; spaces are collapsed and trimmed")
    void dropsSignsAndSpaces() {
        assertThat(ArabicSearchNormalizer.normalize("  ۞ الـلـه   ۚ  أكبر ۩ ", false)).isEqualTo("الله اكبر");
    }

    @Test
    @DisplayName("hasMarks sees vowel marks but not pause marks")
    void detectsMarks() {
        assertThat(ArabicSearchNormalizer.hasMarks("الصَّبر")).isTrue();
        assertThat(ArabicSearchNormalizer.hasMarks("الصبر ۚ")).isFalse();
        assertThat(ArabicSearchNormalizer.hasMarks("al-baqara")).isFalse();
    }
}
