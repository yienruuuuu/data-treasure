package io.github.yienruuuuu.xtracker.dao;

import io.github.yienruuuuu.xtracker.bean.po.XTrackerCrawledPostEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
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
}
