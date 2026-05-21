package io.github.yienruuuuu.scheduler.controller;

import io.github.yienruuuuu.common.bean.dto.ApiErrorResponse;
import io.github.yienruuuuu.scheduler.bean.dto.CreateScheduledTaskRequest;
import io.github.yienruuuuu.scheduler.bean.dto.ScheduledTaskErrorResponse;
import io.github.yienruuuuu.scheduler.bean.dto.ScheduledTaskResponse;
import io.github.yienruuuuu.scheduler.service.ScheduledTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/**
 * REST API for managing persisted scheduler tasks.
 */
@Tag(name = "排程任務", description = "持久化排程任務管理 API，可建立 Cron 任務、查詢狀態、啟用/停用任務與查看錯誤紀錄。")
@RestController
@RequestMapping("/api/scheduled-tasks")
public class ScheduledTaskController {

    private static final int DEFAULT_MAX_ATTEMPTS = 3;

    private final ScheduledTaskService scheduledTaskService;

    public ScheduledTaskController(ScheduledTaskService scheduledTaskService) {
        this.scheduledTaskService = scheduledTaskService;
    }

    @Operation(
            summary = "建立 Cron 排程任務",
            description = """
                    建立一筆持久化排程任務。
                    
                    任務會依 cronExpression 計算下一次執行時間，並由背景排程器自動 claim 與執行。
                    taskType 必須是系統已定義的 enum，例如 DATA_RESEARCH。
                    payload 為 JSON 字串，實際欄位由對應的 ScheduledTaskHandler 定義。
                    """,
            responses = {
                    @ApiResponse(responseCode = "201", description = "建立成功"),
                    @ApiResponse(
                            responseCode = "400",
                            description = "請求格式錯誤、cronExpression 無效或 taskType 不存在",
                            content = @Content(
                                    schema = @Schema(implementation = ApiErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "code": 2001,
                                              "name": "INVALID_ARGUMENT",
                                              "message": "Invalid cron expression",
                                              "timestamp": "2026-05-21T08:00:00Z"
                                            }
                                            """)
                            )
                    )
            }
    )
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

    @Operation(
            summary = "啟用排程任務",
            description = "將指定任務改回 ACTIVE，重置 attempt 與 lock 資訊；如果 nextRunAt 已過期，會重新計算下一次 cron 執行時間。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "啟用成功"),
                    @ApiResponse(responseCode = "404", description = "找不到任務", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    @PatchMapping("/{taskId}/enable")
    public ScheduledTaskResponse enable(
            @Parameter(description = "任務 ID", example = "018f9f2c-2f6a-7b4b-9a4b-6f2b0c5f4d11")
            @PathVariable UUID taskId
    ) {
        scheduledTaskService.enable(taskId);
        return ScheduledTaskResponse.from(scheduledTaskService.getTask(taskId));
    }

    @Operation(
            summary = "停用排程任務",
            description = "將指定任務改為 DISABLED，背景排程器不會再 claim 這筆任務。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "停用成功"),
                    @ApiResponse(responseCode = "404", description = "找不到任務", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    @PatchMapping("/{taskId}/disable")
    public ScheduledTaskResponse disable(
            @Parameter(description = "任務 ID", example = "018f9f2c-2f6a-7b4b-9a4b-6f2b0c5f4d11")
            @PathVariable UUID taskId
    ) {
        scheduledTaskService.disable(taskId);
        return ScheduledTaskResponse.from(scheduledTaskService.getTask(taskId));
    }

    @Operation(
            summary = "查詢排程任務",
            description = "查詢單一排程任務的目前狀態、下一次執行時間、重試次數與 lock 資訊。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查詢成功"),
                    @ApiResponse(responseCode = "404", description = "找不到任務", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    @GetMapping("/{taskId}")
    public ScheduledTaskResponse get(
            @Parameter(description = "任務 ID", example = "018f9f2c-2f6a-7b4b-9a4b-6f2b0c5f4d11")
            @PathVariable UUID taskId
    ) {
        return ScheduledTaskResponse.from(scheduledTaskService.getTask(taskId));
    }

    @Operation(
            summary = "查詢排程任務錯誤紀錄",
            description = "依任務 ID 查詢所有執行失敗紀錄，結果依發生時間由新到舊排序。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查詢成功"),
                    @ApiResponse(responseCode = "404", description = "找不到任務", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    @GetMapping("/{taskId}/errors")
    public List<ScheduledTaskErrorResponse> getErrors(
            @Parameter(description = "任務 ID", example = "018f9f2c-2f6a-7b4b-9a4b-6f2b0c5f4d11")
            @PathVariable UUID taskId
    ) {
        return scheduledTaskService.getErrors(taskId).stream()
                .map(ScheduledTaskErrorResponse::from)
                .toList();
    }
}
