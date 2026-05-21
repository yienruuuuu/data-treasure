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
    void createCronTaskRejectsInvalidCronExpression() {
        assertThatThrownBy(() -> service.createCronTask(ScheduledTaskType.DATA_RESEARCH, "bad cron", "{}", 3))
                .isInstanceOf(BadRequestApiException.class)
                .hasMessageContaining("Invalid cron expression");
    }

    @Test
    void createCronTaskPersistsActiveTask() {
        when(scheduledTaskDao.save(any(ScheduledTaskEntity.class))).thenAnswer(invocation -> {
            ScheduledTaskEntity task = invocation.getArgument(0);
            task.setId(UUID.randomUUID());
            return task;
        });

        UUID taskId = service.createCronTask(ScheduledTaskType.DATA_RESEARCH, "0 * * * * *", "{}", 3);

        assertThat(taskId).isNotNull();
        org.mockito.Mockito.verify(scheduledTaskDao).save(org.mockito.Mockito.argThat(task ->
                task.getTaskType().equals("DATA_RESEARCH")
                        && task.getCronExpression().equals("0 * * * * *")
                        && task.getPayload().equals("{}")
                        && task.getStatus() == ScheduledTaskStatus.ACTIVE
                        && task.getMaxAttempts() == 3
                        && task.getNextRunAt() != null
        ));
    }
}
