package io.github.omarsherifsayed.tajweed.general.schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The envelope of every answer of the API, success or error ({@code AGENTS.md} §9).
 *
 * <p>A success carries {@code data}; an error carries no {@code data} and an error {@code code} from
 * {@link io.github.omarsherifsayed.tajweed.general.exception.ErrorCode}. {@code message} is always in the request's
 * language. There is no time stamp in the body on purpose: the same content must give the same body, so that the
 * {@code ETag} header lets clients and caches reuse an answer (the HTTP {@code Date} header gives the time).
 *
 * @param success {@code true} for a successful answer, {@code false} for an error
 * @param code    {@code "OK"} for a success, otherwise the name of the error code, for example
 *                {@code "NOT_FOUND_PATH"}
 * @param message a short text for people, in the request's language
 * @param data    the answer itself; absent from the JSON on an error
 * @param <T>     the type of {@code data}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "The envelope of every answer: success flag, code, message in the request's language, data.")
public record BaseResponse<T>(boolean success, String code, String message, T data) {

    /** The {@code code} of every successful answer. */
    public static final String OK = "OK";

    /**
     * Builds a successful answer.
     *
     * @param message a short text in the request's language
     * @param data    the answer itself
     * @param <T>     the type of {@code data}
     * @return the envelope
     */
    public static <T> BaseResponse<T> ok(String message, T data) {
        return new BaseResponse<>(true, OK, message, data);
    }

    /**
     * Builds an error answer.
     *
     * @param code    the error code's name
     * @param message the error's text in the request's language
     * @param <T>     the type a success would have carried
     * @return the envelope, without data
     */
    public static <T> BaseResponse<T> error(String code, String message) {
        return new BaseResponse<>(false, code, message, null);
    }
}
