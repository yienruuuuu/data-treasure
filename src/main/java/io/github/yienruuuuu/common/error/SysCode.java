package io.github.yienruuuuu.common.error;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Common system-level response codes shared by API exception handling.
 */
@Getter
@AllArgsConstructor
public enum SysCode implements ErrorCode {

    OK(1000, "Success"),
    FAIL(2000, "Expected error"),
    INVALID_ARGUMENT(2001, "Invalid argument"),
    NOT_FOUND(2002, "Resource not found"),
    INTERNAL_ERROR(5000, "Internal server error");

    private final Integer code;
    private final String message;
}
