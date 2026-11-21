package io.github.omarsherifsayed.tajweed.general.exception;

import java.io.Serial;
import java.util.Arrays;
import lombok.Getter;

/**
 * The exception a service throws when a request cannot be answered; {@link GlobalExceptionHandler} turns it into a
 * {@code BaseResponse} with the code's HTTP status and its message in the request's language.
 *
 * <p>Example: {@code throw new BaseException(ErrorCode.NOT_FOUND_PATH);}
 */
@Getter
public class BaseException extends RuntimeException {

    /** Version of the serialised form (exceptions are serialisable). */
    @Serial
    private static final long serialVersionUID = 1L;

    /** What went wrong, from the API's one list of codes. */
    private final ErrorCode errorCode;

    /** Values for the slots ({@code {0}}, {@code {1}} …) of the code's message, if it has any. */
    private final transient Object[] messageArgs;

    /**
     * Creates the exception.
     *
     * @param errorCode   what went wrong
     * @param messageArgs values for the slots of the code's message, if it has any
     */
    public BaseException(ErrorCode errorCode, Object... messageArgs) {
        super(errorCode.name());
        this.errorCode = errorCode;
        this.messageArgs = messageArgs == null ? new Object[0] : Arrays.copyOf(messageArgs, messageArgs.length);
    }

    /**
     * The values for the slots of the message.
     *
     * @return a copy, so the exception cannot be changed from outside
     */
    public Object[] getMessageArgs() {
        return Arrays.copyOf(messageArgs, messageArgs.length);
    }
}
