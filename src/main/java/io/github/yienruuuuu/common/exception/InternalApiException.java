package io.github.yienruuuuu.common.exception;

import io.github.yienruuuuu.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public class InternalApiException extends ApiException {

    public InternalApiException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
    }
}
