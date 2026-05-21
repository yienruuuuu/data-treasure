package io.github.yienruuuuu.scheduler.dao;

import io.github.yienruuuuu.scheduler.bean.po.ScheduledTaskErrorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Persistence access for scheduled task execution failures.
 */
public interface ScheduledTaskErrorDao extends JpaRepository<ScheduledTaskErrorEntity, UUID> {

    @Query("""
            select error
            from ScheduledTaskErrorEntity error
            join fetch error.task task
            where task.id = :taskId
            order by error.createdAt desc
            """)
    List<ScheduledTaskErrorEntity> findByTaskIdOrderByCreatedAtDesc(@Param("taskId") UUID taskId);
}
