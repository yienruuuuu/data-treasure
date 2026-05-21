package io.github.yienruuuuu.scheduler.entity;

import io.github.yienruuuuu.scheduler.domain.ScheduledTaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "scheduled_task")
public class ScheduledTaskEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "task_type", nullable = false, length = 100)
    private String taskType;

    @Column(name = "cron_expression", nullable = false, length = 120)
    private String cronExpression;

    @Column(name = "payload")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ScheduledTaskStatus status;

    @Column(name = "next_run_at", nullable = false)
    private Instant nextRunAt;

    @Column(name = "attempt", nullable = false)
    private int attempt;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "lock_owner", length = 120)
    private String lockOwner;

    @Column(name = "lock_until")
    private Instant lockUntil;

    @Column(name = "last_started_at")
    private Instant lastStartedAt;

    @Column(name = "last_finished_at")
    private Instant lastFinishedAt;

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
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public ScheduledTaskStatus getStatus() {
        return status;
    }

    public void setStatus(ScheduledTaskStatus status) {
        this.status = status;
    }

    public Instant getNextRunAt() {
        return nextRunAt;
    }

    public void setNextRunAt(Instant nextRunAt) {
        this.nextRunAt = nextRunAt;
    }

    public int getAttempt() {
        return attempt;
    }

    public void setAttempt(int attempt) {
        this.attempt = attempt;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public String getLockOwner() {
        return lockOwner;
    }

    public void setLockOwner(String lockOwner) {
        this.lockOwner = lockOwner;
    }

    public Instant getLockUntil() {
        return lockUntil;
    }

    public void setLockUntil(Instant lockUntil) {
        this.lockUntil = lockUntil;
    }

    public Instant getLastStartedAt() {
        return lastStartedAt;
    }

    public void setLastStartedAt(Instant lastStartedAt) {
        this.lastStartedAt = lastStartedAt;
    }

    public Instant getLastFinishedAt() {
        return lastFinishedAt;
    }

    public void setLastFinishedAt(Instant lastFinishedAt) {
        this.lastFinishedAt = lastFinishedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
