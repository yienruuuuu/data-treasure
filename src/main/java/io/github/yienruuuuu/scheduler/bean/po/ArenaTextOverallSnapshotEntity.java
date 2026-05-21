package io.github.yienruuuuu.scheduler.bean.po;

import io.github.yienruuuuu.scheduler.domain.ArenaCrawlStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "arena_text_overall_snapshot")
public class ArenaTextOverallSnapshotEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "leaderboard_key", nullable = false, length = 100)
    private String leaderboardKey;

    @Column(name = "source_url", nullable = false, length = 500)
    private String sourceUrl;

    @Column(name = "updated_date", nullable = false)
    private LocalDate updatedDate;

    @Column(name = "total_votes", nullable = false)
    private long totalVotes;

    @Column(name = "declared_model_count", nullable = false)
    private int declaredModelCount;

    @Column(name = "fetched_model_count", nullable = false)
    private int fetchedModelCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "crawl_status", nullable = false, length = 30)
    private ArenaCrawlStatus crawlStatus;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (fetchedAt == null) {
            fetchedAt = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
    }
}
