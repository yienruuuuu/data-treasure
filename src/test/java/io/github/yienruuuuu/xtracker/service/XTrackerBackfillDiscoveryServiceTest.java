package io.github.yienruuuuu.xtracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.yienruuuuu.xtracker.bean.dto.XTrackerPostData;
import io.github.yienruuuuu.xtracker.bean.dto.XTrackerPostsFetchResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class XTrackerBackfillDiscoveryServiceTest {

    private final XTrackerFetchService fetchService = mock(XTrackerFetchService.class);
    private final XTrackerPostParseService parseService = mock(XTrackerPostParseService.class);
    private final XTrackerBackfillDiscoveryService service = new XTrackerBackfillDiscoveryService(
            fetchService,
            parseService,
            Instant.parse("2025-09-01T00:00:00Z")
    );
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void discoverEarliestAtScansMonthlyWindowsAndReturnsEarliestPostTime() {
        // given
        XTrackerPostsFetchResult fetchResult = fetchResult();
        when(fetchService.fetchPosts(eq("X"), eq("elonmusk"), any())).thenReturn(fetchResult);
        when(parseService.parsePosts(eq(fetchResult.responseBody()), eq("X"), eq("elonmusk")))
                .thenReturn(List.of())
                .thenReturn(List.of(
                        post("2025-10-31T21:40:43Z"),
                        post("2025-10-31T00:17:35Z")
                ))
                .thenReturn(List.of());

        // when
        Instant earliestAt = service.discoverEarliestAt(
                "X",
                "elonmusk",
                Instant.parse("2025-11-15T00:00:00Z")
        );

        // then
        assertThat(earliestAt).isEqualTo(Instant.parse("2025-10-31T00:17:35Z"));
    }

    private XTrackerPostsFetchResult fetchResult() {
        return new XTrackerPostsFetchResult(
                "/users/elonmusk/posts",
                "https://xtracker.polymarket.com/api/users/elonmusk/posts",
                "X",
                "elonmusk",
                "X:elonmusk",
                objectMapper.createObjectNode(),
                200,
                "{}",
                objectMapper.createObjectNode()
        );
    }

    private XTrackerPostData post(String postedAt) {
        return new XTrackerPostData(
                postedAt,
                "tracker-" + postedAt,
                postedAt,
                Instant.parse(postedAt),
                null,
                "text",
                "https://x.com/elonmusk/status/" + postedAt,
                objectMapper.createObjectNode()
        );
    }
}
