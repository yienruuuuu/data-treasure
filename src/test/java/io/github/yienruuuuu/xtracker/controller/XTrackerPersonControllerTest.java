package io.github.yienruuuuu.xtracker.controller;

import io.github.yienruuuuu.common.controller.GlobalExceptionHandler;
import io.github.yienruuuuu.xtracker.bean.dto.XTrackerActivityTrendResponse;
import io.github.yienruuuuu.xtracker.domain.XTrackerTrackedPerson;
import io.github.yienruuuuu.xtracker.service.XTrackerPersonSyncService;
import io.github.yienruuuuu.xtracker.service.XTrackerPostQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(XTrackerPersonController.class)
@Import(GlobalExceptionHandler.class)
class XTrackerPersonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private XTrackerPersonSyncService syncService;

    @MockBean
    private XTrackerPostQueryService queryService;

    @Test
    void activityTrendReturnsCacheHeadersAndBody() throws Exception {
        // given
        when(queryService.getActivityTrend(eq(XTrackerTrackedPerson.ELON_MUSK), isNull(), isNull(), isNull()))
                .thenReturn(response());

        // when / then
        mockMvc.perform(get("/api/xtracker/persons/tracked/ELON_MUSK/posts/activity-trend"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public, max-age=60, stale-while-revalidate=240"))
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andExpect(jsonPath("$.range.timezone").value("UTC"))
                .andExpect(jsonPath("$.series[0].bucketStartAt").value("2026-05-23T16:00:00Z"));
    }

    @Test
    void activityTrendReturnsNotModifiedWhenEtagMatches() throws Exception {
        // given
        when(queryService.getActivityTrend(eq(XTrackerTrackedPerson.ELON_MUSK), isNull(), isNull(), isNull()))
                .thenReturn(response());
        MvcResult firstResult = mockMvc.perform(get("/api/xtracker/persons/tracked/ELON_MUSK/posts/activity-trend"))
                .andExpect(status().isOk())
                .andReturn();
        String etag = firstResult.getResponse().getHeader(HttpHeaders.ETAG);

        // when / then
        mockMvc.perform(get("/api/xtracker/persons/tracked/ELON_MUSK/posts/activity-trend")
                        .header(HttpHeaders.IF_NONE_MATCH, etag))
                .andExpect(status().isNotModified())
                .andExpect(header().string(HttpHeaders.ETAG, etag))
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).isEmpty());
    }

    private XTrackerActivityTrendResponse response() {
        return new XTrackerActivityTrendResponse(
                new XTrackerActivityTrendResponse.Source("X", "Tweet", "elon-musk", "elonmusk", "Elon Musk"),
                new XTrackerActivityTrendResponse.Range(
                        Instant.parse("2026-05-23T16:00:00Z"),
                        Instant.parse("2026-05-23T17:00:00Z"),
                        "UTC",
                        "hour"
                ),
                new XTrackerActivityTrendResponse.Metrics(3, Instant.parse("2026-05-23T16:00:00Z"), 3, 10),
                List.of(new XTrackerActivityTrendResponse.SeriesPoint(
                        Instant.parse("2026-05-23T16:00:00Z"),
                        Instant.parse("2026-05-23T17:00:00Z"),
                        3,
                        10
                )),
                new XTrackerActivityTrendResponse.Insight("趨勢摘要", "區間內最高單一 bucket 發文量為 3，累積曲線維持上升。")
        );
    }
}
