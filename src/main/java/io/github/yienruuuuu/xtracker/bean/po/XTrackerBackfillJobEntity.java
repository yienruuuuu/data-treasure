package io.github.yienruuuuu.xtracker.bean.po;

import io.github.yienruuuuu.xtracker.domain.XTrackerBackfillStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "xtracker_backfill_job")
public class XTrackerBackfillJobEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "platform", nullable = false, length = 30)
    private String platform;

    @Column(name = "handle", nullable = false, length = 100)
    private String handle;

    @Column(name = "earliest_at", nullable = false)
    private Instant earliestAt;

    @Column(name = "cutoff_at", nullable = false)
    private Instant cutoffAt;

    @Column(name = "enable_realtime_after", nullable = false)
    private boolean enableRealtimeAfter;

    @Column(name = "realtime_task_id")
    private UUID realtimeTaskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private XTrackerBackfillStatus status;

    @Column(name = "total_windows", nullable = false)
    private int totalWindows;

    @Column(name = "completed_windows", nullable = false)
    private int completedWindows;

    @Column(name = "failed_windows", nullable = false)
    private int failedWindows;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
