package io.github.yienruuuuu.scheduler.dao;

import io.github.yienruuuuu.scheduler.domain.ScheduledTaskStatus;
import io.github.yienruuuuu.scheduler.entity.ScheduledTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduledTaskDao extends JpaRepository<ScheduledTaskEntity, UUID> {

    @Query("""
            select task
            from ScheduledTaskEntity task
            where task.id = :id
            """)
    Optional<ScheduledTaskEntity> findTaskById(@Param("id") UUID id);

    @Query(value = """
            select *
            from scheduled_task
            where status in ('ACTIVE', 'RUNNING')
              and next_run_at <= :now
              and (lock_until is null or lock_until < :now)
            order by next_run_at asc
            limit :limit
            for update skip locked
            """, nativeQuery = true)
    List<ScheduledTaskEntity> findDueTasksForUpdate(@Param("now") Instant now, @Param("limit") int limit);

    @Modifying
    @Query("""
            update ScheduledTaskEntity task
            set task.status = :status,
                task.updatedAt = :now
            where task.id = :id
            """)
    int updateStatus(@Param("id") UUID id, @Param("status") ScheduledTaskStatus status, @Param("now") Instant now);
}
