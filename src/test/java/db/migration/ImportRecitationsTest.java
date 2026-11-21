package db.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Proves the checks of the recitation import without a database: how a style is read from mp3quran's label, which
 * timing files pass, and how our CSV files and the timing manifest are read.
 */
class ImportRecitationsTest {

    @Test
    @DisplayName("The style is read from mp3quran's label; a label that names none gives no style")
    void readsTheStyle() {
        assertThat(V1_3_1__ImportRecitations.styleOf("مرتل")).isEqualTo("MURATTAL");
        assertThat(V1_3_1__ImportRecitations.styleOf("المصحف المجود")).isEqualTo("MUJAWWAD");
        assertThat(V1_3_1__ImportRecitations.styleOf("المصحف المعلم")).isEqualTo("MUALLIM");
        assertThat(V1_3_1__ImportRecitations.styleOf("تسجيل عام 1387 هـ - 1967م")).isEmpty();
    }

    @Test
    @DisplayName("Timings pass with ayah 0 or without it, when every ayah is there and the times go forward")
    void acceptsGoodTimings() {
        assertThat(V1_3_1__ImportRecitations.timingProblem(
                List.of(new int[] {0, 0, 10}, new int[] {1, 10, 20}, new int[] {2, 20, 30}), 2)).isNull();
        assertThat(V1_3_1__ImportRecitations.timingProblem(
                List.of(new int[] {1, 5, 20}, new int[] {2, 20, 30}), 2)).isNull();
    }

    @Test
    @DisplayName("Timings fail when the last ayah is missing (al-Husary's Al-Fatihah: 0 – 6 for 7 ayahs)")
    void refusesAMissingAyah() {
        List<int[]> rows = List.of(new int[] {0, 0, 10}, new int[] {1, 10, 20}, new int[] {2, 20, 30},
                new int[] {3, 30, 40}, new int[] {4, 40, 50}, new int[] {5, 50, 60}, new int[] {6, 60, 70});
        assertThat(V1_3_1__ImportRecitations.timingProblem(rows, 7)).contains("rows for 7 ayahs");
    }

    @Test
    @DisplayName("Timings fail when an ayah is out of order, empty, or starts before the one before it")
    void refusesBadTimes() {
        assertThat(V1_3_1__ImportRecitations.timingProblem(
                List.of(new int[] {1, 0, 10}, new int[] {3, 10, 20}), 2)).contains("out of order");
        assertThat(V1_3_1__ImportRecitations.timingProblem(
                List.of(new int[] {1, 0, 10}, new int[] {2, 10, 10}), 2)).contains("times of ayah 2");
        assertThat(V1_3_1__ImportRecitations.timingProblem(
                List.of(new int[] {1, 50, 60}, new int[] {2, 40, 70}), 2)).contains("times of ayah 2");
    }

    @Test
    @DisplayName("Our CSV files: comments and the header are skipped; a row with the wrong number of columns stops")
    void readsCsv() {
        String text = "# a comment\nid,name\n1,a\n2,b\n";
        assertThat(V1_3_1__ImportRecitations.csv(text, 2)).hasSize(2);
        assertThatThrownBy(() -> V1_3_1__ImportRecitations.csv(text + "3\n", 2))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("The audio check counts a file as working only with 200 and an audio content type")
    void readsTheAudioCheck() {
        String text = "provider,key,surah,ayah,status,bytes,content_type\n"
                + "MP3QURAN,112,1,0,200,840426,audio/mpeg\nMP3QURAN,112,2,0,404,0,text/html\n"
                + "MP3QURAN,112,3,0,200,10,text/html\n";
        assertThat(V1_3_1__ImportRecitations.audioCheck(text))
                .containsEntry("MP3QURAN|112|1|0", true)
                .containsEntry("MP3QURAN|112|2|0", false)
                .containsEntry("MP3QURAN|112|3|0", false);
    }

    @Test
    @DisplayName("The timing manifest maps each file to its SHA-256; a malformed line stops")
    void readsTheManifest() {
        String sha = "a".repeat(64);
        assertThat(V1_3_1__ImportRecitations.manifest(sha + "  112/001.json\n")).containsEntry("112/001.json", sha);
        assertThatThrownBy(() -> V1_3_1__ImportRecitations.manifest("xyz  112/001.json\n"))
                .isInstanceOf(IllegalStateException.class);
    }
}
