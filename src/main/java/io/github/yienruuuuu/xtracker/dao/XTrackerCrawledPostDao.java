package io.github.yienruuuuu.xtracker.dao;

import io.github.yienruuuuu.xtracker.bean.po.XTrackerCrawledPostEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface XTrackerCrawledPostDao extends JpaRepository<XTrackerCrawledPostEntity, UUID> {

    Optional<XTrackerCrawledPostEntity> findByPersonIdAndSourcePostId(UUID personId, String sourcePostId);

    @Query("""
            select count(post)
            from XTrackerCrawledPostEntity post
            where post.person.id = :personId
              and post.postedAt >= :startAt
              and post.postedAt < :endAt
            """)
    long countByPersonIdAndPostedAtRange(
            @Param("personId") UUID personId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt
    );

    @Query(value = """
            with buckets as (
                select
                    cast(:startAt as timestamp with time zone)
                        + (step_index * cast(:bucketInterval as interval)) as bucket_start_at,
                    least(
                        cast(:startAt as timestamp with time zone)
                            + ((step_index + 1) * cast(:bucketInterval as interval)),
                        cast(:endAt as timestamp with time zone)
                    ) as bucket_end_at
                from generate_series(0, :bucketCount - 1) as steps(step_index)
            ),
            bucket_counts as (
                select
                    bucket_start_at,
                    bucket_end_at,
                    count(post.id) as daily_count
                from buckets
                left join xtracker_crawled_post post
                    on post.person_id = :personId
                   and post.posted_at >= bucket_start_at
                   and post.posted_at < bucket_end_at
                group by bucket_start_at, bucket_end_at
            )
            select
                bucket_start_at as bucketStartAt,
                bucket_end_at as bucketEndAt,
                daily_count as dailyCount,
                (
                    select count(all_post.id)
                    from xtracker_crawled_post all_post
                    where all_post.person_id = :personId
                      and all_post.posted_at < bucket_end_at
                ) as cumulativeCount
            from bucket_counts
            order by bucket_start_at
            """, nativeQuery = true)
    List<ActivityTrendBucketRow> findActivityTrendBuckets(
            @Param("personId") UUID personId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt,
            @Param("bucketInterval") String bucketInterval,
            @Param("bucketCount") int bucketCount
    );

    interface ActivityTrendBucketRow {
        Timestamp getBucketStartAt();

        Timestamp getBucketEndAt();

        long getDailyCount();

        long getCumulativeCount();
    }
}
