package io.github.yienruuuuu.common.controller;

import java.time.Instant;

public record ApiErrorResponse(
        Integer code,
        String name,
        String message,
        Instant timestamp
) {
}
