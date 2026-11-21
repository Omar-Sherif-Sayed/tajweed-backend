package io.github.omarsherifsayed.tajweed.general.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Every error the API can answer with — one list for the whole backend, grouped by prefix ({@code AGENTS.md} §9).
 *
 * <p>Each code has its HTTP status and a message key; the key exists in all 22 message files
 * ({@code i18n/messages_*.properties}), which a test checks. The three clients show the message, or their own text
 * for the code. A code is never renamed once published: clients depend on the names.
 */
@Getter
public enum ErrorCode {

    /** The path does not exist (no endpoint answers it). */
    NOT_FOUND_PATH(HttpStatus.NOT_FOUND, "error.not_found_path"),

    /** The request is malformed: a missing or invalid parameter, a body that cannot be read. */
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "error.invalid_request"),

    /** The path exists, but not with this HTTP method (the API is read-only: almost everything is GET). */
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "error.method_not_allowed"),

    /** The client sent too many requests in a minute (see {@code tajweed.rate-limit}). */
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "error.rate_limited"),

    /** An unexpected failure on our side; the details are in the server log, never in the answer. */
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "error.internal_error");

    /** The HTTP status the answer carries. */
    private final HttpStatus status;

    /** The key of the message in the {@code i18n/messages_*.properties} files. */
    private final String messageKey;

    /**
     * Declares an error code.
     *
     * @param status     the HTTP status of the answer
     * @param messageKey the key of its message in the message files
     */
    ErrorCode(HttpStatus status, String messageKey) {
        this.status = status;
        this.messageKey = messageKey;
    }
}
