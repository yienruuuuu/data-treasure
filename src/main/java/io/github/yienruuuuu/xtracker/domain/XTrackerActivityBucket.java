package io.github.yienruuuuu.xtracker.domain;

import io.github.yienruuuuu.common.error.SysCode;
import io.github.yienruuuuu.common.exception.BadRequestApiException;

import java.time.Duration;

/**
 * Supported public chart aggregation intervals.
 */
public enum XTrackerActivityBucket {
    HOUR("hour", Duration.ofHours(1)),
    DAY("day", Duration.ofDays(1));

    private final String apiValue;
    private final Duration duration;

    XTrackerActivityBucket(String apiValue, Duration duration) {
        this.apiValue = apiValue;
        this.duration = duration;
    }

    public String apiValue() {
        return apiValue;
    }

    public Duration duration() {
        return duration;
    }

    public static XTrackerActivityBucket from(String value) {
        if (value == null || value.isBlank()) {
            return HOUR;
        }
        for (XTrackerActivityBucket bucket : values()) {
            if (bucket.apiValue.equalsIgnoreCase(value.trim()) || bucket.name().equalsIgnoreCase(value.trim())) {
                return bucket;
            }
        }
        throw new BadRequestApiException(SysCode.INVALID_ARGUMENT, "Unsupported bucket: " + value);
    }
}
