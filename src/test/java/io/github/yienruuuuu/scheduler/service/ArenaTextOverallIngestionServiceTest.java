package io.github.yienruuuuu.scheduler.service;

import io.github.yienruuuuu.common.exception.InternalApiException;
import io.github.yienruuuuu.scheduler.bean.dto.ArenaTextOverallItemData;
import io.github.yienruuuuu.scheduler.bean.dto.ArenaTextOverallSnapshotData;
import io.github.yienruuuuu.scheduler.bean.po.ArenaTextOverallSnapshotEntity;
import io.github.yienruuuuu.scheduler.dao.ArenaTextOverallItemDao;
import io.github.yienruuuuu.scheduler.dao.ArenaTextOverallSnapshotDao;
import io.github.yienruuuuu.scheduler.domain.ArenaCrawlStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArenaTextOverallIngestionServiceTest {

    private final ArenaTextOverallSnapshotDao snapshotDao = mock(ArenaTextOverallSnapshotDao.class);
    private final ArenaTextOverallItemDao itemDao = mock(ArenaTextOverallItemDao.class);
    private final ArenaTextOverallIngestionService service = new ArenaTextOverallIngestionService(snapshotDao, itemDao);

    @Test
    void ingestWritesPartialSnapshotWhenFetchedIsLowerThanDeclared() {
        // given
        ArenaTextOverallSnapshotData parsed = new ArenaTextOverallSnapshotData(
                "text_overall",
                "https://arena.ai/leaderboard/text",
                LocalDate.of(2026, 5, 18),
                6_297_180L,
                360,
                List.of(item(1), item(2))
        );
        when(snapshotDao.findByLeaderboardKeyAndUpdatedDate("text_overall", LocalDate.of(2026, 5, 18)))
                .thenReturn(Optional.empty());

        // when
        service.ingest(parsed);

        // then
        ArgumentCaptor<ArenaTextOverallSnapshotEntity> snapshotCaptor = ArgumentCaptor.forClass(ArenaTextOverallSnapshotEntity.class);
        verify(snapshotDao).save(snapshotCaptor.capture());
        assertThat(snapshotCaptor.getValue().getCrawlStatus()).isEqualTo(ArenaCrawlStatus.PARTIAL);
        assertThat(snapshotCaptor.getValue().getFetchedModelCount()).isEqualTo(2);
        verify(itemDao).saveAll(any());
    }

    @Test
    void ingestSkipsWhenSnapshotAlreadyExists() {
        // given
        ArenaTextOverallSnapshotData parsed = new ArenaTextOverallSnapshotData(
                "text_overall",
                "https://arena.ai/leaderboard/text",
                LocalDate.of(2026, 5, 18),
                6_297_180L,
                360,
                List.of(item(1))
        );
        when(snapshotDao.findByLeaderboardKeyAndUpdatedDate("text_overall", LocalDate.of(2026, 5, 18)))
                .thenReturn(Optional.of(new ArenaTextOverallSnapshotEntity()));

        // when
        service.ingest(parsed);

        // then
        verify(snapshotDao, never()).save(any());
        verify(itemDao, never()).saveAll(any());
    }

    @Test
    void ingestThrowsWhenFetchedItemsAreZero() {
        // given
        ArenaTextOverallSnapshotData parsed = new ArenaTextOverallSnapshotData(
                "text_overall",
                "https://arena.ai/leaderboard/text",
                LocalDate.of(2026, 5, 18),
                6_297_180L,
                360,
                List.of()
        );
        when(snapshotDao.findByLeaderboardKeyAndUpdatedDate("text_overall", LocalDate.of(2026, 5, 18)))
                .thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> service.ingest(parsed))
                .isInstanceOf(InternalApiException.class)
                .hasMessageContaining("zero items");
        verify(snapshotDao, never()).save(any());
        verify(itemDao, never()).saveAll(any());
    }

    private ArenaTextOverallItemData item(int rank) {
        return new ArenaTextOverallItemData(
                rank,
                rank,
                rank + 1,
                "model-" + rank,
                "provider",
                "Proprietary",
                1400,
                5,
                1000,
                null,
                null,
                "1M",
                false,
                "https://example.com/model-" + rank
        );
    }
}
