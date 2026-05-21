package io.github.yienruuuuu.common.exception;

import io.github.yienruuuuu.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public class BadRequestApiException extends ApiException {

    public BadRequestApiException(ErrorCode errorCode) {
        super(errorCode, HttpStatus.BAD_REQUEST);
    }

    public BadRequestApiException(ErrorCode errorCode, String message) {
        super(errorCode, HttpStatus.BAD_REQUEST, message);
    }
}
