package io.github.yienruuuuu.scheduler.bean.dto;

import java.time.LocalDate;
import java.util.List;

public record ArenaTextOverallSnapshotData(
        String leaderboardKey,
        String sourceUrl,
        LocalDate updatedDate,
        long totalVotes,
        int declaredModelCount,
        List<ArenaTextOverallItemData> items
) {
}
