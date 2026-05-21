package io.github.yienruuuuu.common.exception;

import io.github.yienruuuuu.common.error.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base runtime exception for expected API failures.
 *
 * <p>Controllers should let this exception propagate to {@code GlobalExceptionHandler}
 * instead of catching it locally.</p>
 */
@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;

    public ApiException(ErrorCode errorCode, HttpStatus httpStatus) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public ApiException(ErrorCode errorCode, HttpStatus httpStatus, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public ApiException(ErrorCode errorCode, HttpStatus httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
