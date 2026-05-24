package io.github.yienruuuuu.xtracker.handler;

import io.github.yienruuuuu.scheduler.domain.ScheduledTaskContext;
import io.github.yienruuuuu.scheduler.domain.ScheduledTaskHandler;
import io.github.yienruuuuu.scheduler.domain.ScheduledTaskType;
import io.github.yienruuuuu.scheduler.service.ScheduledTaskService;
import io.github.yienruuuuu.xtracker.service.XTrackerBackfillJobService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class XTrackerBackfillWorkerTaskHandler implements ScheduledTaskHandler {

    private final XTrackerBackfillJobService backfillJobService;
    private final ScheduledTaskService scheduledTaskService;

    public XTrackerBackfillWorkerTaskHandler(
            XTrackerBackfillJobService backfillJobService,
            ScheduledTaskService scheduledTaskService
    ) {
        this.backfillJobService = backfillJobService;
        this.scheduledTaskService = scheduledTaskService;
    }

    @Override
    public ScheduledTaskType taskType() {
        return ScheduledTaskType.XTRACKER_PERSON_POST_BACKFILL_WORKER;
    }

    @Override
    public void handle(ScheduledTaskContext context) {
        log.info("Scheduled XTracker backfill worker started. taskId={}", context.taskId());
        backfillJobService.processDueWindows();
        if (!backfillJobService.hasActiveBackfillWork()) {
            scheduledTaskService.disable(context.taskId());
            log.info("Scheduled XTracker backfill worker disabled because no active backfill windows remain. taskId={}",
                    context.taskId());
        }
        log.info("Scheduled XTracker backfill worker completed. taskId={}", context.taskId());
    }
}
