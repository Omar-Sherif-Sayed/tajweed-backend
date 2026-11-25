package io.github.omarsherifsayed.tajweed.business.tafsir.client;

import lombok.Getter;

/**
 * An answer of the Quran Foundation's API that is not {@code 200}, or a request that could not be sent.
 */
@Getter
public class QuranFoundationException extends RuntimeException {

    /** The HTTP status, or 0 when no answer came. */
    private final int status;

    /** The API's error type ({@code resync_required}, {@code rate_limit_exceeded} …), or empty. */
    private final String type;

    /**
     * Creates it.
     *
     * @param status the HTTP status, or 0
     * @param type   the API's error type, or empty
     * @param where  what was called (without its query: no token or key is ever in a message), and the API's own
     *               error message when it gives one
     */
    public QuranFoundationException(int status, String type, String where) {
        super("Quran Foundation answered " + status + " " + type + " for " + where);
        this.status = status;
        this.type = type;
    }
}
