package io.github.yienruuuuu.xtracker.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.yienruuuuu.scheduler.domain.ScheduledTaskContext;
import io.github.yienruuuuu.scheduler.domain.ScheduledTaskHandler;
import io.github.yienruuuuu.scheduler.domain.ScheduledTaskType;
import io.github.yienruuuuu.xtracker.service.XTrackerPersonSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class XTrackerRealtimeSyncTaskHandler implements ScheduledTaskHandler {

    private static final String DEFAULT_PLATFORM = "X";
    private static final String DEFAULT_HANDLE = "elonmusk";

    private final XTrackerPersonSyncService syncService;
    private final ObjectMapper objectMapper;

    public XTrackerRealtimeSyncTaskHandler(XTrackerPersonSyncService syncService, ObjectMapper objectMapper) {
        this.syncService = syncService;
        this.objectMapper = objectMapper;
    }

    @Override
    public ScheduledTaskType taskType() {
        return ScheduledTaskType.XTRACKER_PERSON_POST_REALTIME_SYNC;
    }

    @Override
    public void handle(ScheduledTaskContext context) {
        Payload payload = resolvePayload(context.payload());
        log.info("Scheduled XTracker realtime sync started. platform={}, handle={}", payload.platform(), payload.handle());
        syncService.syncOnce(payload.platform(), payload.handle(), false);
        log.info("Scheduled XTracker realtime sync completed. platform={}, handle={}", payload.platform(), payload.handle());
    }

    private Payload resolvePayload(String rawPayload) {
        if (rawPayload == null || rawPayload.isBlank()) {
            return new Payload(DEFAULT_PLATFORM, DEFAULT_HANDLE);
        }
        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            String platform = root.path("platform").isTextual() && !root.path("platform").asText().isBlank()
                    ? root.path("platform").asText()
                    : DEFAULT_PLATFORM;
            String handle = root.path("handle").isTextual() && !root.path("handle").asText().isBlank()
                    ? root.path("handle").asText()
                    : DEFAULT_HANDLE;
            return new Payload(platform, handle);
        } catch (Exception exception) {
            log.warn("Invalid XTracker realtime scheduler payload, fallback to Elon. payload={}", rawPayload);
            return new Payload(DEFAULT_PLATFORM, DEFAULT_HANDLE);
        }
    }

    private record Payload(String platform, String handle) {
    }
}
