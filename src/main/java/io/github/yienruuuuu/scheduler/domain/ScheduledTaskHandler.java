package io.github.yienruuuuu.scheduler.domain;

/**
 * Extension point for executable scheduled tasks.
 *
 * <p>Each handler owns one task type and should contain only the business logic
 * for that task. Retry, locking, and error persistence are handled by the
 * scheduler framework.</p>
 */
public interface ScheduledTaskHandler {

    ScheduledTaskType taskType();

    void handle(ScheduledTaskContext context);
}
