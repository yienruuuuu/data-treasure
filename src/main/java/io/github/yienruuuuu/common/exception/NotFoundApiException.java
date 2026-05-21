package io.github.yienruuuuu.common.exception;

import io.github.yienruuuuu.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public class NotFoundApiException extends ApiException {

    public NotFoundApiException(ErrorCode errorCode, String message) {
        super(errorCode, HttpStatus.NOT_FOUND, message);
    }
}
