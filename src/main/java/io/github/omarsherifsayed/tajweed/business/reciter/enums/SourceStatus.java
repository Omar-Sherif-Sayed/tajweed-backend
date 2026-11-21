package io.github.omarsherifsayed.tajweed.business.reciter.enums;

/**
 * The result of the health check of a source.
 */
public enum SourceStatus {

    /** The sample file answered {@code 200} with audio at the last check. */
    OK,

    /** Three checks in a row failed. The clients still try it, after the others (F11). */
    FAILING
}
