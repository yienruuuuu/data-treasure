package io.github.yienruuuuu.scheduler.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.yienruuuuu.scheduler.bean.dto.DataResearchPayload;
import io.github.yienruuuuu.scheduler.domain.ScheduledTaskContext;
import io.github.yienruuuuu.scheduler.domain.ScheduledTaskType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataResearchTaskHandlerTest {

    private final DataResearchTaskHandler handler = new DataResearchTaskHandler(new ObjectMapper());

    @Test
    void taskTypeIsDataResearch() {
        assertThat(handler.taskType()).isEqualTo(ScheduledTaskType.DATA_RESEARCH);
    }

    @Test
    void handleAcceptsValidPayload() {
        ScheduledTaskContext context = new ScheduledTaskContext(
                UUID.randomUUID(),
                ScheduledTaskType.DATA_RESEARCH,
                "{\"subject\":\"scheduler framework\",\"depth\":\"deep\",\"fail\":false}",
                1
        );

        handler.handle(context);
    }

    @Test
    void handleThrowsWhenPayloadRequestsFailure() {
        ScheduledTaskContext context = new ScheduledTaskContext(
                UUID.randomUUID(),
                ScheduledTaskType.DATA_RESEARCH,
                "{\"subject\":\"scheduler framework\",\"depth\":\"deep\",\"fail\":true}",
                1
        );

        assertThatThrownBy(() -> handler.handle(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("failed by payload flag");
    }
}
