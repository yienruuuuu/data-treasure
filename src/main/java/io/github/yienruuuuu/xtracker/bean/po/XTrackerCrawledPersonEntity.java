package io.github.yienruuuuu.xtracker.bean.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
@Table(name = "xtracker_crawled_person")
public class XTrackerCrawledPersonEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "platform", nullable = false, length = 30)
    private String platform;

    @Column(name = "handle", nullable = false, length = 100)
    private String handle;

    @Column(name = "source_user_id", length = 128)
    private String sourceUserId;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "profile_url")
    private String profileUrl;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

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
