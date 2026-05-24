package io.github.yienruuuuu.xtracker.bean.po;

import io.github.yienruuuuu.xtracker.domain.XTrackerBackfillStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "xtracker_backfill_window")
public class XTrackerBackfillWindowEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private XTrackerBackfillJobEntity job;

    @Column(name = "platform", nullable = false, length = 30)
    private String platform;

    @Column(name = "handle", nullable = false, length = 100)
    private String handle;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private XTrackerBackfillStatus status;

    @Column(name = "attempt", nullable = false)
    private int attempt;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "next_run_at", nullable = false)
    private Instant nextRunAt;

    @Column(name = "lock_owner", length = 120)
    private String lockOwner;

    @Column(name = "lock_until")
    private Instant lockUntil;

    @Column(name = "last_started_at")
    private Instant lastStartedAt;

    @Column(name = "last_finished_at")
    private Instant lastFinishedAt;

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
        if (nextRunAt == null) {
            nextRunAt = now;
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
