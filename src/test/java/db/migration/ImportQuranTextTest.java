package db.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Proves the safety checks of the Quran import without a database: a changed file is refused by its checksum, a
 * correction that does not match the metadata is refused, the text lines are kept exactly as written, and the basmala
 * is recognised by its letters — also in 95:1 and 97:1, where Tanzil writes it with a shadda.
 */
class ImportQuranTextTest {

    @Test
    @DisplayName("The three source files in the application have their recorded checksums")
    void sourceFilesAreUnchanged() throws Exception {
        assertThat(V1_1_1__ImportQuranText.read(V1_1_1__ImportQuranText.TEXT_FILE,
                V1_1_1__ImportQuranText.TEXT_SHA256)).isNotEmpty();
        assertThat(V1_1_1__ImportQuranText.read(V1_1_1__ImportQuranText.META_FILE,
                V1_1_1__ImportQuranText.META_SHA256)).isNotEmpty();
        assertThat(V1_1_1__ImportQuranText.read(V1_1_1__ImportQuranText.CORRECTIONS_FILE,
                V1_1_1__ImportQuranText.CORRECTIONS_SHA256)).isNotEmpty();
    }

    @Test
    @DisplayName("A file whose checksum differs stops the import")
    void refusesAChangedFile() {
        assertThatThrownBy(() -> V1_1_1__ImportQuranText.read(V1_1_1__ImportQuranText.TEXT_FILE, "0".repeat(64)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("checksum");
    }

    @Test
    @DisplayName("Text lines are kept exactly as written; comments and empty lines are skipped")
    void parsesTheTextVerbatim() {
        Map<Integer, Map<Integer, String>> ayahs = V1_1_1__ImportQuranText.parseText(
                "1|1|نص  الآية | كما هو\n# a comment\n\n1|2|ثانية\n");
        assertThat(ayahs.get(1)).containsEntry(1, "نص  الآية | كما هو").containsEntry(2, "ثانية");
    }

    @Test
    @DisplayName("A correction is applied only where the metadata has the stated start")
    void appliesOnlyMatchingCorrections() {
        List<int[]> quarters = new ArrayList<>(List.of(new int[] {15, 1}, new int[] {15, 50}));
        String ok = "quarter,tanzil_surah,tanzil_ayah,mushaf_surah,mushaf_ayah,evidence\n2,15,50,15,49,evidence\n";
        assertThat(V1_1_1__ImportQuranText.applyCorrections(quarters, ok).get(1)).containsExactly(15, 49);

        String wrong = "2,15,51,15,49,evidence\n";
        assertThatThrownBy(() -> V1_1_1__ImportQuranText.applyCorrections(quarters, wrong))
                .isInstanceOf(IllegalStateException.class);
        String noEvidence = "2,15,50,15,49,\n";
        assertThatThrownBy(() -> V1_1_1__ImportQuranText.applyCorrections(quarters, noEvidence))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("The basmala of 95:1, written with a shadda, has the same letters as that of 1:1")
    void recognisesTheBasmalaByItsLetters() {
        String fatiha = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ";
        String tin = "بِّسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ";
        assertThat(tin).isNotEqualTo(fatiha);
        assertThat(V1_1_1__ImportQuranText.letters(tin)).isEqualTo(V1_1_1__ImportQuranText.letters(fatiha));
    }
}
