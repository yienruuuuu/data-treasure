package io.github.yienruuuuu.xtracker.service;

import io.github.yienruuuuu.common.exception.BadRequestApiException;
import io.github.yienruuuuu.common.exception.NotFoundApiException;
import io.github.yienruuuuu.xtracker.bean.dto.XTrackerActivityTrendResponse;
import io.github.yienruuuuu.xtracker.bean.dto.XTrackerPostCountResponse;
import io.github.yienruuuuu.xtracker.bean.po.XTrackerCrawledPersonEntity;
import io.github.yienruuuuu.xtracker.dao.XTrackerCrawledPersonDao;
import io.github.yienruuuuu.xtracker.dao.XTrackerCrawledPostDao;
import io.github.yienruuuuu.xtracker.domain.XTrackerTrackedPerson;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class XTrackerPostQueryServiceTest {

    private final XTrackerCrawledPersonDao personDao = mock(XTrackerCrawledPersonDao.class);
    private final XTrackerCrawledPostDao postDao = mock(XTrackerCrawledPostDao.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-24T13:10:30Z"), ZoneOffset.UTC);
    private final XTrackerPostQueryService service = new XTrackerPostQueryService(personDao, postDao, clock);

    @Test
    void countPostsReturnsRangeCountForPerson() {
        // given
        XTrackerCrawledPersonEntity person = new XTrackerCrawledPersonEntity();
        person.setId(UUID.randomUUID());
        person.setPlatform("X");
        person.setHandle("elonmusk");
        Instant startAt = Instant.parse("2026-05-21T00:00:00Z");
        Instant endAt = Instant.parse("2026-05-21T01:00:00Z");
        when(personDao.findByPlatformAndHandle("X", "elonmusk")).thenReturn(Optional.of(person));
        when(postDao.countByPersonIdAndPostedAtRange(person.getId(), startAt, endAt)).thenReturn(7L);

        // when
        XTrackerPostCountResponse response = service.countPosts("x", "@elonmusk", startAt, endAt);

        // then
        assertThat(response.postCount()).isEqualTo(7);
        assertThat(response.startAt()).isEqualTo(startAt);
        assertThat(response.endAt()).isEqualTo(endAt);
    }

    @Test
    void countPostsRejectsInvalidRange() {
        // given
        Instant startAt = Instant.parse("2026-05-21T01:00:00Z");
        Instant endAt = Instant.parse("2026-05-21T00:00:00Z");

        // when / then
        assertThatThrownBy(() -> service.countPosts("X", "elonmusk", startAt, endAt))
                .isInstanceOf(BadRequestApiException.class)
                .hasMessageContaining("startAt");
    }

    @Test
    void countPostsThrowsWhenPersonDoesNotExist() {
        // given
        Instant startAt = Instant.parse("2026-05-21T00:00:00Z");
        Instant endAt = Instant.parse("2026-05-21T01:00:00Z");
        when(personDao.findByPlatformAndHandle("X", "elonmusk")).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> service.countPosts("X", "elonmusk", startAt, endAt))
                .isInstanceOf(NotFoundApiException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void activityTrendReturnsUtcRangeMetricsAndSeries() {
        // given
        XTrackerCrawledPersonEntity person = person("Elon Musk");
        Instant startAt = Instant.parse("2026-05-23T16:00:00Z");
        Instant endAt = Instant.parse("2026-05-23T18:00:00Z");
        when(personDao.findByPlatformAndHandle("X", "elonmusk")).thenReturn(Optional.of(person));
        when(postDao.findActivityTrendBuckets(person.getId(), startAt, endAt, "1 hour", 2))
                .thenReturn(List.of(
                        row("2026-05-23T16:00:00Z", "2026-05-23T17:00:00Z", 2, 10),
                        row("2026-05-23T17:00:00Z", "2026-05-23T18:00:00Z", 5, 15)
                ));

        // when
        XTrackerActivityTrendResponse response = service.getActivityTrend(
                XTrackerTrackedPerson.ELON_MUSK,
                startAt,
                endAt,
                "hour"
        );

        // then
        assertThat(response.source().personId()).isEqualTo("elon-musk");
        assertThat(response.range().startAt()).isEqualTo(startAt);
        assertThat(response.range().endAt()).isEqualTo(endAt);
        assertThat(response.range().timezone()).isEqualTo("UTC");
        assertThat(response.metrics().totalCount()).isEqualTo(7);
        assertThat(response.metrics().peakBucketStartAt()).isEqualTo(Instant.parse("2026-05-23T17:00:00Z"));
        assertThat(response.metrics().peakBucketCount()).isEqualTo(5);
        assertThat(response.metrics().cumulativeEndCount()).isEqualTo(15);
        assertThat(response.series()).hasSize(2);
        assertThat(response.series().get(0).bucketStartAt()).isEqualTo(Instant.parse("2026-05-23T16:00:00Z"));
    }

    @Test
    void activityTrendUsesDefaultLast24HoursWhenRangeMissing() {
        // given
        XTrackerCrawledPersonEntity person = person("Elon Musk");
        Instant expectedEndAt = Instant.parse("2026-05-24T13:10:00Z");
        Instant expectedStartAt = Instant.parse("2026-05-23T13:10:00Z");
        when(personDao.findByPlatformAndHandle("X", "elonmusk")).thenReturn(Optional.of(person));
        when(postDao.findActivityTrendBuckets(person.getId(), expectedStartAt, expectedEndAt, "1 hour", 24))
                .thenReturn(List.of(row("2026-05-23T13:10:00Z", "2026-05-23T14:10:00Z", 0, 3)));

        // when
        XTrackerActivityTrendResponse response = service.getActivityTrend(
                XTrackerTrackedPerson.ELON_MUSK,
                null,
                null,
                null
        );

        // then
        assertThat(response.range().startAt()).isEqualTo(expectedStartAt);
        assertThat(response.range().endAt()).isEqualTo(expectedEndAt);
        assertThat(response.range().bucket()).isEqualTo("hour");
    }

    @Test
    void activityTrendCachesNormalizedQuery() {
        // given
        XTrackerCrawledPersonEntity person = person("Elon Musk");
        Instant startAt = Instant.parse("2026-05-23T16:00:00Z");
        Instant endAt = Instant.parse("2026-05-23T18:00:00Z");
        when(personDao.findByPlatformAndHandle("X", "elonmusk")).thenReturn(Optional.of(person));
        when(postDao.findActivityTrendBuckets(person.getId(), startAt, endAt, "1 hour", 2))
                .thenReturn(List.of(row("2026-05-23T16:00:00Z", "2026-05-23T17:00:00Z", 1, 1)));

        // when
        service.getActivityTrend(XTrackerTrackedPerson.ELON_MUSK, startAt, endAt, "hour");
        service.getActivityTrend(XTrackerTrackedPerson.ELON_MUSK, startAt, endAt, "hour");

        // then
        verify(postDao, times(1)).findActivityTrendBuckets(person.getId(), startAt, endAt, "1 hour", 2);
    }

    @Test
    void activityTrendRejectsPartialRange() {
        // given
        Instant startAt = Instant.parse("2026-05-23T16:00:00Z");

        // when / then
        assertThatThrownBy(() -> service.getActivityTrend(XTrackerTrackedPerson.ELON_MUSK, startAt, null, "hour"))
                .isInstanceOf(BadRequestApiException.class)
                .hasMessageContaining("together");
    }

    @Test
    void activityTrendRejectsRangeOverThirtyDays() {
        // given
        Instant startAt = Instant.parse("2026-05-01T00:00:00Z");
        Instant endAt = Instant.parse("2026-06-01T00:00:00Z");

        // when / then
        assertThatThrownBy(() -> service.getActivityTrend(XTrackerTrackedPerson.ELON_MUSK, startAt, endAt, "hour"))
                .isInstanceOf(BadRequestApiException.class)
                .hasMessageContaining("30 days");
    }

    private XTrackerCrawledPersonEntity person(String displayName) {
        XTrackerCrawledPersonEntity person = new XTrackerCrawledPersonEntity();
        person.setId(UUID.randomUUID());
        person.setPlatform("X");
        person.setHandle("elonmusk");
        person.setDisplayName(displayName);
        return person;
    }

    private XTrackerCrawledPostDao.ActivityTrendBucketRow row(
            String bucketStartAt,
            String bucketEndAt,
            long dailyCount,
            long cumulativeCount
    ) {
        return new XTrackerCrawledPostDao.ActivityTrendBucketRow() {
            @Override
            public Timestamp getBucketStartAt() {
                return Timestamp.from(Instant.parse(bucketStartAt));
            }

            @Override
            public Timestamp getBucketEndAt() {
                return Timestamp.from(Instant.parse(bucketEndAt));
            }

            @Override
            public long getDailyCount() {
                return dailyCount;
            }

            @Override
            public long getCumulativeCount() {
                return cumulativeCount;
            }
        };
    }
}
