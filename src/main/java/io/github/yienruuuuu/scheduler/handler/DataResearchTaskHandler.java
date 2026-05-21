package io.github.yienruuuuu.scheduler.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.yienruuuuu.scheduler.bean.dto.DataResearchPayload;
import io.github.yienruuuuu.scheduler.domain.ScheduledTaskContext;
import io.github.yienruuuuu.scheduler.domain.ScheduledTaskHandler;
import io.github.yienruuuuu.scheduler.domain.ScheduledTaskType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Demo implementation for the {@link ScheduledTaskType#DATA_RESEARCH} task type.
 *
 * <p>The {@code fail} payload flag intentionally throws an exception so retry
 * and error persistence can be exercised end to end.</p>
 */
@Slf4j
@Component
public class DataResearchTaskHandler implements ScheduledTaskHandler {

    private final ObjectMapper objectMapper;

    public DataResearchTaskHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ScheduledTaskType taskType() {
        return ScheduledTaskType.DATA_RESEARCH;
    }

    @Override
    public void handle(ScheduledTaskContext context) {
        DataResearchPayload payload = parsePayload(context.payload());
        if (payload.fail()) {
            throw new IllegalStateException("Demo data research task failed by payload flag");
        }

        log.info(
                "Demo data research task executed. taskId={}, subject={}, depth={}, attempt={}",
                context.taskId(),
                payload.normalizedSubject(),
                payload.normalizedDepth(),
                context.attempt()
        );
    }

    private DataResearchPayload parsePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return new DataResearchPayload(null, null, false);
        }

        try {
            return objectMapper.readValue(payload, DataResearchPayload.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid DATA_RESEARCH payload", ex);
        }
    }
}
