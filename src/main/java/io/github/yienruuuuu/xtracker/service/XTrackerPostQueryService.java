package io.github.yienruuuuu.xtracker.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.yienruuuuu.common.error.SysCode;
import io.github.yienruuuuu.common.exception.BadRequestApiException;
import io.github.yienruuuuu.common.exception.NotFoundApiException;
import io.github.yienruuuuu.xtracker.bean.dto.XTrackerActivityTrendResponse;
import io.github.yienruuuuu.xtracker.bean.dto.XTrackerPostCountResponse;
import io.github.yienruuuuu.xtracker.bean.po.XTrackerCrawledPersonEntity;
import io.github.yienruuuuu.xtracker.dao.XTrackerCrawledPersonDao;
import io.github.yienruuuuu.xtracker.dao.XTrackerCrawledPostDao;
import io.github.yienruuuuu.xtracker.domain.XTrackerActivityBucket;
import io.github.yienruuuuu.xtracker.domain.XTrackerTrackedPerson;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Service
public class XTrackerPostQueryService {

    private static final String DEFAULT_PLATFORM = "X";
    private static final String SOURCE_LABEL = "Tweet";
    private static final String RESPONSE_TIMEZONE = "UTC";
    private static final Duration DEFAULT_RANGE = Duration.ofHours(24);
    private static final Duration MAX_RANGE = Duration.ofDays(30);
    private static final int MAX_BUCKETS = 720;

    private final XTrackerCrawledPersonDao personDao;
    private final XTrackerCrawledPostDao postDao;
    private final Clock clock;
    private final Cache<ActivityTrendCacheKey, XTrackerActivityTrendResponse> activityTrendCache;

    public XTrackerPostQueryService(XTrackerCrawledPersonDao personDao, XTrackerCrawledPostDao postDao, Clock clock) {
        this.personDao = personDao;
        this.postDao = postDao;
        this.clock = clock;
        this.activityTrendCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(60))
                .maximumSize(2_000)
                .build();
    }

    public XTrackerPostCountResponse countPosts(String platform, String handle, Instant startAt, Instant endAt) {
        String normalizedPlatform = normalizePlatform(platform);
        String normalizedHandle = normalizeHandle(handle);
        if (startAt == null || endAt == null || !startAt.isBefore(endAt)) {
            throw new BadRequestApiException(SysCode.INVALID_ARGUMENT, "startAt must be before endAt");
        }
        XTrackerCrawledPersonEntity person = personDao.findByPlatformAndHandle(normalizedPlatform, normalizedHandle)
                .orElseThrow(() -> new NotFoundApiException(
                        SysCode.NOT_FOUND,
                        "XTracker person not found: " + normalizedPlatform + "/" + normalizedHandle
                ));
        long count = postDao.countByPersonIdAndPostedAtRange(person.getId(), startAt, endAt);
        return new XTrackerPostCountResponse(normalizedPlatform, normalizedHandle, startAt, endAt, count);
    }

    public XTrackerActivityTrendResponse getActivityTrend(
            XTrackerTrackedPerson trackedPerson,
            Instant startAt,
            Instant endAt,
            String bucketValue
    ) {
        XTrackerActivityBucket bucket = XTrackerActivityBucket.from(bucketValue);
        ActivityTrendRange range = normalizeRange(startAt, endAt, bucket);
        ActivityTrendCacheKey cacheKey = new ActivityTrendCacheKey(
                trackedPerson.platform(),
                trackedPerson.handle(),
                range.startAt(),
                range.endAt(),
                bucket.apiValue()
        );
        return activityTrendCache.get(cacheKey, ignored -> buildActivityTrend(trackedPerson, range, bucket));
    }

    private XTrackerActivityTrendResponse buildActivityTrend(
            XTrackerTrackedPerson trackedPerson,
            ActivityTrendRange range,
            XTrackerActivityBucket bucket
    ) {
        XTrackerCrawledPersonEntity person = personDao.findByPlatformAndHandle(trackedPerson.platform(), trackedPerson.handle())
                .orElseThrow(() -> new NotFoundApiException(
                        SysCode.NOT_FOUND,
                        "XTracker person not found: " + trackedPerson.platform() + "/" + trackedPerson.handle()
                ));
        List<XTrackerActivityTrendResponse.SeriesPoint> series = postDao.findActivityTrendBuckets(
                        person.getId(),
                        range.startAt(),
                        range.endAt(),
                        bucketInterval(bucket),
                        range.bucketCount()
                )
                .stream()
                .map(row -> new XTrackerActivityTrendResponse.SeriesPoint(
                        row.getBucketStartAt().toInstant(),
                        row.getBucketEndAt().toInstant(),
                        row.getDailyCount(),
                        row.getCumulativeCount()
                ))
                .toList();

        long totalCount = series.stream()
                .mapToLong(XTrackerActivityTrendResponse.SeriesPoint::dailyCount)
                .sum();
        XTrackerActivityTrendResponse.SeriesPoint peak = series.stream()
                .max(Comparator.comparingLong(XTrackerActivityTrendResponse.SeriesPoint::dailyCount))
                .orElse(null);
        long peakBucketCount = peak == null ? 0 : peak.dailyCount();
        Instant peakBucketStartAt = peak == null || peakBucketCount == 0 ? null : peak.bucketStartAt();
        long cumulativeEndCount = series.isEmpty() ? 0 : series.get(series.size() - 1).cumulativeCount();

        return new XTrackerActivityTrendResponse(
                new XTrackerActivityTrendResponse.Source(
                        person.getPlatform(),
                        SOURCE_LABEL,
                        personId(trackedPerson),
                        person.getHandle(),
                        displayName(person, trackedPerson)
                ),
                new XTrackerActivityTrendResponse.Range(
                        range.startAt(),
                        range.endAt(),
                        RESPONSE_TIMEZONE,
                        bucket.apiValue()
                ),
                new XTrackerActivityTrendResponse.Metrics(
                        totalCount,
                        peakBucketStartAt,
                        peakBucketCount,
                        cumulativeEndCount
                ),
                series,
                insight(totalCount, peakBucketCount)
        );
    }

    private ActivityTrendRange normalizeRange(Instant startAt, Instant endAt, XTrackerActivityBucket bucket) {
        Instant normalizedStartAt = startAt;
        Instant normalizedEndAt = endAt;
        if (normalizedStartAt == null && normalizedEndAt == null) {
            normalizedEndAt = Instant.now(clock).truncatedTo(ChronoUnit.MINUTES);
            normalizedStartAt = normalizedEndAt.minus(DEFAULT_RANGE);
        } else if (normalizedStartAt == null || normalizedEndAt == null) {
            throw new BadRequestApiException(SysCode.INVALID_ARGUMENT, "startAt and endAt must be provided together");
        }
        if (!normalizedStartAt.isBefore(normalizedEndAt)) {
            throw new BadRequestApiException(SysCode.INVALID_ARGUMENT, "startAt must be before endAt");
        }
        Duration rangeDuration = Duration.between(normalizedStartAt, normalizedEndAt);
        if (rangeDuration.compareTo(MAX_RANGE) > 0) {
            throw new BadRequestApiException(SysCode.INVALID_ARGUMENT, "Activity trend range must not exceed 30 days");
        }
        int bucketCount = bucketCount(rangeDuration, bucket.duration());
        if (bucketCount > MAX_BUCKETS) {
            throw new BadRequestApiException(SysCode.INVALID_ARGUMENT, "Activity trend range produces too many buckets");
        }
        return new ActivityTrendRange(normalizedStartAt, normalizedEndAt, bucketCount);
    }

    private int bucketCount(Duration rangeDuration, Duration bucketDuration) {
        long rangeMillis = rangeDuration.toMillis();
        long bucketMillis = bucketDuration.toMillis();
        return Math.toIntExact((rangeMillis + bucketMillis - 1) / bucketMillis);
    }

    private String bucketInterval(XTrackerActivityBucket bucket) {
        return switch (bucket) {
            case HOUR -> "1 hour";
            case DAY -> "1 day";
        };
    }

    private String personId(XTrackerTrackedPerson trackedPerson) {
        return trackedPerson.name().toLowerCase().replace('_', '-');
    }

    private String displayName(XTrackerCrawledPersonEntity person, XTrackerTrackedPerson trackedPerson) {
        if (person.getDisplayName() != null && !person.getDisplayName().isBlank()) {
            return person.getDisplayName();
        }
        if (trackedPerson == XTrackerTrackedPerson.ELON_MUSK) {
            return "Elon Musk";
        }
        return person.getHandle();
    }

    private XTrackerActivityTrendResponse.Insight insight(long totalCount, long peakBucketCount) {
        String body = totalCount == 0
                ? "此區間尚未觀測到發文資料。"
                : "區間內出現發文高峰，累積曲線維持上升。";
        if (peakBucketCount > 0) {
            body = "區間內最高單一 bucket 發文量為 " + peakBucketCount + "，累積曲線維持上升。";
        }
        return new XTrackerActivityTrendResponse.Insight("趨勢摘要", body);
    }

    private String normalizePlatform(String platform) {
        if (platform == null || platform.isBlank()) {
            return DEFAULT_PLATFORM;
        }
        return platform.trim().toUpperCase();
    }

    private String normalizeHandle(String handle) {
        if (handle == null || handle.isBlank()) {
            throw new BadRequestApiException(SysCode.INVALID_ARGUMENT, "XTracker handle is required");
        }
        return handle.trim().replaceFirst("^@", "");
    }

    private record ActivityTrendRange(Instant startAt, Instant endAt, int bucketCount) {
    }

    private record ActivityTrendCacheKey(String platform, String handle, Instant startAt, Instant endAt, String bucket) {
    }
}
