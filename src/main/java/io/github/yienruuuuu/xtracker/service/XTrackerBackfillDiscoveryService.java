package io.github.yienruuuuu.xtracker.service;

import io.github.yienruuuuu.common.error.SysCode;
import io.github.yienruuuuu.common.exception.BadRequestApiException;
import io.github.yienruuuuu.xtracker.bean.dto.XTrackerPostData;
import io.github.yienruuuuu.xtracker.bean.dto.XTrackerPostsFetchResult;
import io.github.yienruuuuu.xtracker.bean.dto.XTrackerSyncOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

/**
 * Discovers the earliest available XTracker post timestamp for backfill jobs.
 */
@Slf4j
@Service
public class XTrackerBackfillDiscoveryService {

    private final XTrackerFetchService fetchService;
    private final XTrackerPostParseService parseService;
    private final Instant searchLowerBound;

    public XTrackerBackfillDiscoveryService(
            XTrackerFetchService fetchService,
            XTrackerPostParseService parseService,
            @Value("${xtracker.backfill.discovery-lower-bound:2020-01-01T00:00:00Z}") Instant searchLowerBound
    ) {
        this.fetchService = fetchService;
        this.parseService = parseService;
        this.searchLowerBound = searchLowerBound;
    }

    public Instant discoverEarliestAt(String platform, String handle, Instant cutoffAt) {
        if (!searchLowerBound.isBefore(cutoffAt)) {
            throw new BadRequestApiException(SysCode.INVALID_ARGUMENT, "discovery lower bound must be before cutoffAt");
        }

        Instant earliestAt = null;
        LocalDate cursorMonth = LocalDate.ofInstant(cutoffAt, ZoneOffset.UTC)
                .withDayOfMonth(1)
                .plusMonths(1);
        log.info("XTracker backfill discovery started. platform={}, handle={}, lowerBound={}, cutoffAt={}",
                platform, handle, searchLowerBound, cutoffAt);
        while (cursorMonth.atStartOfDay().toInstant(ZoneOffset.UTC).isAfter(searchLowerBound)) {
            Instant cursorStartAt = cursorMonth.atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant endAt = cursorStartAt.isBefore(cutoffAt) ? cursorStartAt : cutoffAt;
            Instant startAt = cursorMonth.minusMonths(1).atStartOfDay().toInstant(ZoneOffset.UTC);
            if (startAt.isBefore(searchLowerBound)) {
                startAt = searchLowerBound;
            }
            if (!startAt.isBefore(endAt)) {
                cursorMonth = cursorMonth.minusMonths(1);
                continue;
            }

            List<XTrackerPostData> posts = fetchWindow(platform, handle, startAt, endAt);
            Instant windowEarliest = posts.stream()
                    .map(XTrackerPostData::postedAt)
                    .min(Comparator.naturalOrder())
                    .orElse(null);
            log.info("XTracker backfill discovery window checked. platform={}, handle={}, startAt={}, endAt={}, count={}, earliestAt={}",
                    platform, handle, startAt, endAt, posts.size(), windowEarliest);
            if (windowEarliest != null && (earliestAt == null || windowEarliest.isBefore(earliestAt))) {
                earliestAt = windowEarliest;
            }
            cursorMonth = cursorMonth.minusMonths(1);
        }

        if (earliestAt == null) {
            throw new BadRequestApiException(SysCode.INVALID_ARGUMENT, "No XTracker posts found for backfill target");
        }
        log.info("XTracker backfill discovery completed. platform={}, handle={}, earliestAt={}, lowerBound={}, cutoffAt={}",
                platform, handle, earliestAt, searchLowerBound, cutoffAt);
        return earliestAt;
    }

    private List<XTrackerPostData> fetchWindow(String platform, String handle, Instant startAt, Instant endAt) {
        XTrackerPostsFetchResult result = fetchService.fetchPosts(
                platform,
                handle,
                new XTrackerSyncOptions(false, startAt, endAt, null)
        );
        return parseService.parsePosts(result.responseBody(), platform, handle);
    }

}
