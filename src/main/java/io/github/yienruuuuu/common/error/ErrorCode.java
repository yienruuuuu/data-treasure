package io.github.yienruuuuu.common.error;

/**
 * Stable API error contract used by exception handling and response mapping.
 */
public interface ErrorCode {

    String getMessage();

    Integer getCode();

    String name();
}
