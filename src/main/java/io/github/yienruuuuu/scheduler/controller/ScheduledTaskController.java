package io.github.yienruuuuu.scheduler.controller;

import io.github.yienruuuuu.scheduler.service.ScheduledTaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/scheduled-tasks")
public class ScheduledTaskController {

    private static final int DEFAULT_MAX_ATTEMPTS = 3;

    private final ScheduledTaskService scheduledTaskService;

    public ScheduledTaskController(ScheduledTaskService scheduledTaskService) {
        this.scheduledTaskService = scheduledTaskService;
    }

    @PostMapping
    public ResponseEntity<ScheduledTaskResponse> create(@Valid @RequestBody CreateScheduledTaskRequest request) {
        int maxAttempts = request.maxAttempts() == null ? DEFAULT_MAX_ATTEMPTS : request.maxAttempts();
        UUID taskId = scheduledTaskService.createCronTask(
                request.taskType(),
                request.cronExpression(),
                request.payload(),
                maxAttempts
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ScheduledTaskResponse.from(scheduledTaskService.getTask(taskId)));
    }

    @PatchMapping("/{taskId}/enable")
    public ScheduledTaskResponse enable(@PathVariable UUID taskId) {
        scheduledTaskService.enable(taskId);
        return ScheduledTaskResponse.from(scheduledTaskService.getTask(taskId));
    }

    @PatchMapping("/{taskId}/disable")
    public ScheduledTaskResponse disable(@PathVariable UUID taskId) {
        scheduledTaskService.disable(taskId);
        return ScheduledTaskResponse.from(scheduledTaskService.getTask(taskId));
    }

    @GetMapping("/{taskId}")
    public ScheduledTaskResponse get(@PathVariable UUID taskId) {
        return ScheduledTaskResponse.from(scheduledTaskService.getTask(taskId));
    }

    @GetMapping("/{taskId}/errors")
    public List<ScheduledTaskErrorResponse> getErrors(@PathVariable UUID taskId) {
        return scheduledTaskService.getErrors(taskId).stream()
                .map(ScheduledTaskErrorResponse::from)
                .toList();
    }
}
