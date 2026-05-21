package io.github.yienruuuuu.scheduler.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.yienruuuuu.scheduler.bean.dto.ArenaTextOverallSnapshotData;
import io.github.yienruuuuu.scheduler.bean.dto.ArenaTextOverallSyncPayload;
import io.github.yienruuuuu.scheduler.domain.ScheduledTaskContext;
import io.github.yienruuuuu.scheduler.domain.ScheduledTaskHandler;
import io.github.yienruuuuu.scheduler.domain.ScheduledTaskType;
import io.github.yienruuuuu.scheduler.service.ArenaTextOverallSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Synchronizes Arena Text Overall leaderboard into snapshot/item tables.
 */
@Slf4j
@Component
public class ArenaTextOverallSyncTaskHandler implements ScheduledTaskHandler {

    private final ArenaTextOverallSyncService syncService;
    private final ObjectMapper objectMapper;

    public ArenaTextOverallSyncTaskHandler(
            ArenaTextOverallSyncService syncService,
            ObjectMapper objectMapper
    ) {
        this.syncService = syncService;
        this.objectMapper = objectMapper;
    }

    @Override
    public ScheduledTaskType taskType() {
        return ScheduledTaskType.ARENA_TEXT_OVERALL_SYNC;
    }

    @Override
    public void handle(ScheduledTaskContext context) {
        ArenaTextOverallSyncPayload payload = parsePayload(context.payload());
        ArenaTextOverallSnapshotData parsed = syncService.syncOnce(payload.sourceUrl());

        log.info("Arena text overall sync completed. taskId={}, sourceUrl={}, updatedDate={}, fetched={}",
                context.taskId(), parsed.sourceUrl(), parsed.updatedDate(), parsed.items().size());
    }

    private ArenaTextOverallSyncPayload parsePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return new ArenaTextOverallSyncPayload(null);
        }

        try {
            return objectMapper.readValue(payload, ArenaTextOverallSyncPayload.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid ARENA_TEXT_OVERALL_SYNC payload", exception);
        }
    }
}
