package io.github.yienruuuuu.scheduler.service;

import io.github.yienruuuuu.scheduler.domain.ScheduledTaskContext;
import io.github.yienruuuuu.scheduler.domain.ScheduledTaskHandler;
import io.github.yienruuuuu.scheduler.domain.ScheduledTaskType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScheduledTaskRegistryTest {

    @Test
    void findReturnsHandlerByTaskType() {
        ScheduledTaskHandler handler = new TestHandler(ScheduledTaskType.DATA_RESEARCH);
        ScheduledTaskRegistry registry = new ScheduledTaskRegistry(List.of(handler));

        assertThat(registry.find("DATA_RESEARCH")).containsSame(handler);
        assertThat(registry.find("missing")).isEmpty();
    }

    @Test
    void constructorRejectsDuplicateTaskType() {
        ScheduledTaskHandler first = new TestHandler(ScheduledTaskType.DATA_RESEARCH);
        ScheduledTaskHandler second = new TestHandler(ScheduledTaskType.DATA_RESEARCH);

        assertThatThrownBy(() -> new ScheduledTaskRegistry(List.of(first, second)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate scheduled task handler");
    }

    private record TestHandler(ScheduledTaskType taskType) implements ScheduledTaskHandler {

        @Override
        public void handle(ScheduledTaskContext context) {
        }
    }
}
