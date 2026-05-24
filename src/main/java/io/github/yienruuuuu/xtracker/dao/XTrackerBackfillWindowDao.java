package io.github.yienruuuuu.xtracker.dao;

import io.github.yienruuuuu.xtracker.bean.po.XTrackerBackfillWindowEntity;
import io.github.yienruuuuu.xtracker.domain.XTrackerBackfillStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface XTrackerBackfillWindowDao extends JpaRepository<XTrackerBackfillWindowEntity, UUID> {

    @Query(value = """
            select *
            from xtracker_backfill_window
            where status in ('PENDING', 'RUNNING')
              and next_run_at <= :now
              and (lock_until is null or lock_until < :now)
            order by start_at asc
            limit :limit
            for update skip locked
            """, nativeQuery = true)
    /*
     * Native PostgreSQL locking keeps multiple app instances from processing
     * the same historical window at the same time.
     */
    List<XTrackerBackfillWindowEntity> findDueWindowsForUpdate(@Param("now") Instant now, @Param("limit") int limit);

    long countByJobIdAndStatus(UUID jobId, XTrackerBackfillStatus status);

    long countByJobId(UUID jobId);

    long countByStatusIn(Collection<XTrackerBackfillStatus> statuses);
}
