package io.github.yienruuuuu.scheduler.dao;

import io.github.yienruuuuu.scheduler.bean.po.ArenaTextOverallSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface ArenaTextOverallSnapshotDao extends JpaRepository<ArenaTextOverallSnapshotEntity, UUID> {

    @Query("""
            select snapshot
            from ArenaTextOverallSnapshotEntity snapshot
            where snapshot.leaderboardKey = :leaderboardKey
              and snapshot.updatedDate = :updatedDate
            """)
    Optional<ArenaTextOverallSnapshotEntity> findByLeaderboardKeyAndUpdatedDate(
            @Param("leaderboardKey") String leaderboardKey,
            @Param("updatedDate") LocalDate updatedDate
    );
}
