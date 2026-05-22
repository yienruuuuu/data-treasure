package io.github.yienruuuuu.xtracker.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.yienruuuuu.scheduler.domain.ScheduledTaskContext;
import io.github.yienruuuuu.scheduler.domain.ScheduledTaskHandler;
import io.github.yienruuuuu.scheduler.domain.ScheduledTaskType;
import io.github.yienruuuuu.xtracker.service.XTrackerPersonSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Scheduler handler that syncs all configured XTracker people for a platform.
 */
@Slf4j
@Component
public class XTrackerPersonPostSyncTaskHandler implements ScheduledTaskHandler {

    private static final String DEFAULT_PLATFORM = "X";

    private final XTrackerPersonSyncService syncService;
    private final ObjectMapper objectMapper;

    public XTrackerPersonPostSyncTaskHandler(XTrackerPersonSyncService syncService, ObjectMapper objectMapper) {
        this.syncService = syncService;
        this.objectMapper = objectMapper;
    }

    @Override
    public ScheduledTaskType taskType() {
        return ScheduledTaskType.XTRACKER_PERSON_POST_SYNC;
    }

    @Override
    public void handle(ScheduledTaskContext context) {
        String platform = resolvePlatform(context.payload());
        log.info("Scheduled XTracker person post sync started. platform={}", platform);
        int synced = syncService.syncAll(platform).size();
        log.info("Scheduled XTracker person post sync completed. platform={}, synced={}", platform, synced);
    }

    private String resolvePlatform(String payload) {
        if (payload == null || payload.isBlank()) {
            return DEFAULT_PLATFORM;
        }
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode platform = root.path("platform");
            return platform.isTextual() && !platform.asText().isBlank() ? platform.asText() : DEFAULT_PLATFORM;
        } catch (Exception exception) {
            log.warn("Invalid XTracker scheduler payload, fallback to default platform. payload={}", payload);
            return DEFAULT_PLATFORM;
        }
    }
}
