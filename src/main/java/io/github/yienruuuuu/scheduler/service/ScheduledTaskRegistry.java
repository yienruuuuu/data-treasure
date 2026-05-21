package io.github.yienruuuuu.scheduler.service;

import io.github.yienruuuuu.scheduler.domain.ScheduledTaskHandler;
import io.github.yienruuuuu.scheduler.domain.ScheduledTaskType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Maps enum-managed task types to their Spring-managed handlers.
 */
@Component
public class ScheduledTaskRegistry {

    private final Map<ScheduledTaskType, ScheduledTaskHandler> handlers;

    public ScheduledTaskRegistry(List<ScheduledTaskHandler> handlers) {
        this.handlers = new HashMap<>();
        for (ScheduledTaskHandler handler : handlers) {
            ScheduledTaskHandler previous = this.handlers.put(handler.taskType(), handler);
            if (previous != null) {
                throw new IllegalStateException("Duplicate scheduled task handler: " + handler.taskType());
            }
        }
    }

    public Optional<ScheduledTaskHandler> find(String taskType) {
        return ScheduledTaskType.findByName(taskType)
                .map(handlers::get);
    }
}
