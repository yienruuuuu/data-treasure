package io.github.yienruuuuu.scheduler.service;

import io.github.yienruuuuu.common.exception.BadRequestApiException;
import io.github.yienruuuuu.scheduler.dao.ScheduledTaskDao;
import io.github.yienruuuuu.scheduler.dao.ScheduledTaskErrorDao;
import io.github.yienruuuuu.scheduler.domain.ScheduledTaskStatus;
import io.github.yienruuuuu.scheduler.domain.ScheduledTaskType;
import io.github.yienruuuuu.scheduler.bean.po.ScheduledTaskEntity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScheduledTaskServiceTest {

    private final ScheduledTaskDao scheduledTaskDao = mock(ScheduledTaskDao.class);
    private final ScheduledTaskErrorDao scheduledTaskErrorDao = mock(ScheduledTaskErrorDao.class);
    private final ScheduledTaskService service = new ScheduledTaskService(scheduledTaskDao, scheduledTaskErrorDao);

    @Test
    void createCronTaskRejectsInvalidMaxAttempts() {
        assertThatThrownBy(() -> service.createCronTask(ScheduledTaskType.ARENA_TEXT_OVERALL_SYNC, "{}", 0))
                .isInstanceOf(BadRequestApiException.class)
                .hasMessageContaining("maxAttempts must be greater than 0");
    }

    @Test
    void createCronTaskPersistsActiveTask() {
        when(scheduledTaskDao.save(any(ScheduledTaskEntity.class))).thenAnswer(invocation -> {
            ScheduledTaskEntity task = invocation.getArgument(0);
            task.setId(UUID.randomUUID());
            return task;
        });

        UUID taskId = service.createCronTask(ScheduledTaskType.ARENA_TEXT_OVERALL_SYNC, "{}", 3);

        assertThat(taskId).isNotNull();
        org.mockito.Mockito.verify(scheduledTaskDao).save(org.mockito.Mockito.argThat(task ->
                task.getTaskType().equals("ARENA_TEXT_OVERALL_SYNC")
                        && task.getCronExpression().equals("0 0 8 * * *")
                        && task.getPayload().equals("{}")
                        && task.getStatus() == ScheduledTaskStatus.ACTIVE
                        && task.getMaxAttempts() == 3
                        && task.getNextRunAt() != null
        ));
    }
}
