package io.github.yienruuuuu.xtracker.bean.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class XTrackerSyncOptionsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void absoluteDateRangeDoesNotForwardTimezoneToXTracker() {
        // given
        XTrackerSyncOptions options = new XTrackerSyncOptions(
                false,
                Instant.parse("2026-05-15T16:00:00Z"),
                Instant.parse("2026-05-22T16:00:00Z"),
                "EST"
        );

        // when
        var requestParams = options.toRequestParams(objectMapper, "X");

        // then
        assertThat(options.shouldForwardTimezone()).isFalse();
        assertThat(requestParams.has("timezone")).isFalse();
        assertThat(options.sourceObjectId("X", "elonmusk"))
                .isEqualTo("X:elonmusk:start=2026-05-15T16:00:00Z:end=2026-05-22T16:00:00Z:timezone=");
    }
}
