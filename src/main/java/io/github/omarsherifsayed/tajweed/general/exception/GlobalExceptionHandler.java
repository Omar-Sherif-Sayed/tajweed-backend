package io.github.omarsherifsayed.tajweed.general.exception;

import io.github.omarsherifsayed.tajweed.general.i18n.MessageService;
import io.github.omarsherifsayed.tajweed.general.schema.BaseResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Turns every error into a {@link BaseResponse} with an {@link ErrorCode} and a message in the request's language.
 *
 * <p>Rules ({@code AGENTS.md} §9): an exception is never swallowed — an unexpected one is logged with its stack trace
 * and answered with {@link ErrorCode#INTERNAL_ERROR}; the answer never carries internal details; logs carry
 * identifiers, not request bodies.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    /** Reads the error messages in the request's language. */
    private final MessageService messages;

    /**
     * An error a service raised on purpose.
     *
     * @param ex the exception, carrying its error code
     * @return the error answer
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<BaseResponse<Void>> handleBase(BaseException ex) {
        return answer(ex.getErrorCode(), ex.getMessageArgs());
    }

    /**
     * A path no endpoint answers.
     *
     * @param ex the exception Spring MVC raises for an unknown path
     * @return a {@link ErrorCode#NOT_FOUND_PATH} answer
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<BaseResponse<Void>> handleNotFound(Exception ex) {
        return answer(ErrorCode.NOT_FOUND_PATH);
    }

    /**
     * A known path called with a method it does not support.
     *
     * @param ex the exception Spring MVC raises
     * @return a {@link ErrorCode#METHOD_NOT_ALLOWED} answer
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<BaseResponse<Void>> handleMethod(HttpRequestMethodNotSupportedException ex) {
        return answer(ErrorCode.METHOD_NOT_ALLOWED);
    }

    /**
     * A malformed request: a missing, invalid or wrongly typed parameter, or an unreadable body.
     *
     * @param ex the exception Spring MVC or bean validation raises
     * @return a {@link ErrorCode#INVALID_REQUEST} answer
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, HandlerMethodValidationException.class,
            ConstraintViolationException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<BaseResponse<Void>> handleInvalid(Exception ex) {
        log.debug("Invalid request: {}", ex.getClass().getSimpleName());
        return answer(ErrorCode.INVALID_REQUEST);
    }

    /**
     * Anything unexpected: logged in full, answered without details.
     *
     * @param ex the exception
     * @return a {@link ErrorCode#INTERNAL_ERROR} answer
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return answer(ErrorCode.INTERNAL_ERROR);
    }

    /**
     * Builds the error answer of a code.
     *
     * @param code the error code
     * @param args values for the slots of its message
     * @return the answer with the code's HTTP status
     */
    private ResponseEntity<BaseResponse<Void>> answer(ErrorCode code, Object... args) {
        String text = messages.get(code.getMessageKey(), args);
        return ResponseEntity.status(code.getStatus()).body(BaseResponse.error(code.name(), text));
    }
}
