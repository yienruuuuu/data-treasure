package io.github.yienruuuuu.xtracker.bean.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "xtracker_crawled_post")
public class XTrackerCrawledPostEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private XTrackerCrawledPersonEntity person;

    @Column(name = "source_post_id", nullable = false, length = 128)
    private String sourcePostId;

    @Column(name = "posted_at", nullable = false)
    private Instant postedAt;

    @Column(name = "text")
    private String text;

    @Column(name = "post_url")
    private String postUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raw_snapshot_id")
    private XTrackerRawApiSnapshotEntity rawSnapshot;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
