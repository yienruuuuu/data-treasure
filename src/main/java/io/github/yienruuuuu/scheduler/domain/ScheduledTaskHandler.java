package io.github.yienruuuuu.scheduler.domain;

public interface ScheduledTaskHandler {

    ScheduledTaskType taskType();

    void handle(ScheduledTaskContext context);
}
