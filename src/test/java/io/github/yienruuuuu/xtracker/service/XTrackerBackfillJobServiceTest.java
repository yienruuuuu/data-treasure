package io.github.yienruuuuu.xtracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.yienruuuuu.scheduler.dao.ScheduledTaskDao;
import io.github.yienruuuuu.scheduler.dao.ScheduledTaskErrorDao;
import io.github.yienruuuuu.scheduler.domain.ScheduledTaskStatus;
import io.github.yienruuuuu.scheduler.domain.ScheduledTaskType;
import io.github.yienruuuuu.scheduler.service.ScheduledTaskService;
import io.github.yienruuuuu.xtracker.dao.XTrackerBackfillJobDao;
import io.github.yienruuuuu.xtracker.dao.XTrackerBackfillWindowDao;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class XTrackerBackfillJobServiceTest {

    private final XTrackerBackfillJobDao jobDao = mock(XTrackerBackfillJobDao.class);
    private final XTrackerBackfillWindowDao windowDao = mock(XTrackerBackfillWindowDao.class);
    private final XTrackerPersonSyncService syncService = mock(XTrackerPersonSyncService.class);
    private final XTrackerBackfillDiscoveryService discoveryService = mock(XTrackerBackfillDiscoveryService.class);
    private final ScheduledTaskDao scheduledTaskDao = mock(ScheduledTaskDao.class);
    private final ScheduledTaskErrorDao scheduledTaskErrorDao = mock(ScheduledTaskErrorDao.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final ScheduledTaskService scheduledTaskService = new ScheduledTaskService(scheduledTaskDao, scheduledTaskErrorDao);
    private final XTrackerBackfillJobService service = new XTrackerBackfillJobService(
            jobDao,
            windowDao,
            syncService,
            discoveryService,
            scheduledTaskService,
            new ObjectMapper(),
            transactionManager,
            3,
            Duration.ofMinutes(5)
    );

    @Test
    void buildDailyUtcWindowsKeepsFirstAndFinalPartialWindows() {
        // given
        Instant earliestAt = Instant.parse("2025-10-31T00:17:35Z");
        Instant cutoffAt = Instant.parse("2025-11-02T03:00:00Z");

        // when
        var windows = service.buildDailyUtcWindows(earliestAt, cutoffAt);

        // then
        assertThat(windows).hasSize(3);
        assertThat(windows.get(0).startAt()).isEqualTo(Instant.parse("2025-10-31T00:17:35Z"));
        assertThat(windows.get(0).endAt()).isEqualTo(Instant.parse("2025-11-01T00:00:00Z"));
        assertThat(windows.get(1).startAt()).isEqualTo(Instant.parse("2025-11-01T00:00:00Z"));
        assertThat(windows.get(1).endAt()).isEqualTo(Instant.parse("2025-11-02T00:00:00Z"));
        assertThat(windows.get(2).startAt()).isEqualTo(Instant.parse("2025-11-02T00:00:00Z"));
        assertThat(windows.get(2).endAt()).isEqualTo(cutoffAt);
    }

    @Test
    void scheduledTaskTypesUseExpectedBackfillAndRealtimeCadence() {
        assertThat(ScheduledTaskType.XTRACKER_PERSON_POST_BACKFILL_WORKER.cronExpression()).isEqualTo("*/10 * * * * *");
        assertThat(ScheduledTaskType.XTRACKER_PERSON_POST_REALTIME_SYNC.cronExpression()).isEqualTo("0 */5 * * * *");
        assertThat(ScheduledTaskStatus.DISABLED).isNotNull();
    }
}
