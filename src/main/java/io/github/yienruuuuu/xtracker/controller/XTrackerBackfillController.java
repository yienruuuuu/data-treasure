package io.github.yienruuuuu.xtracker.controller;

import io.github.yienruuuuu.common.bean.dto.ApiErrorResponse;
import io.github.yienruuuuu.xtracker.bean.dto.XTrackerBackfillJobRequest;
import io.github.yienruuuuu.xtracker.bean.dto.XTrackerBackfillJobResponse;
import io.github.yienruuuuu.xtracker.bean.dto.XTrackerTrackedBackfillJobRequest;
import io.github.yienruuuuu.xtracker.domain.XTrackerTrackedPerson;
import io.github.yienruuuuu.xtracker.service.XTrackerBackfillJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "XTracker 歷史補資料", description = "建立 XTracker 人物歷史資料 backfill job，完成後可啟用即時探測任務。")
@RestController
@RequestMapping("/api/xtracker/backfill-jobs")
public class XTrackerBackfillController {

    private final XTrackerBackfillJobService backfillJobService;

    public XTrackerBackfillController(XTrackerBackfillJobService backfillJobService) {
        this.backfillJobService = backfillJobService;
    }

    @Operation(
            summary = "建立 XTracker 歷史補資料任務",
            description = "建立 Elon 或指定人物的歷史補資料 job，切成 UTC 半開時間窗分批處理；未提供 earliestAt 時會先向 XTracker 按月往前探測最早可用資料時間。",
            responses = {
                    @ApiResponse(responseCode = "201", description = "建立成功"),
                    @ApiResponse(
                            responseCode = "400",
                            description = "請求參數錯誤",
                            content = @Content(
                                    schema = @Schema(implementation = ApiErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "code": 2001,
                                              "name": "INVALID_ARGUMENT",
                                              "message": "earliestAt must be before current time",
                                              "timestamp": "2026-05-23T08:00:00Z"
                                            }
                                            """)
                            )
                    )
            }
    )
    @PostMapping
    public ResponseEntity<XTrackerBackfillJobResponse> create(@RequestBody(required = false) XTrackerBackfillJobRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(XTrackerBackfillJobResponse.from(backfillJobService.createJob(request)));
    }

    @Operation(
            summary = "建立預設追蹤人物的 XTracker 歷史補資料任務",
            description = "使用 enum 管理的追蹤人物建立 backfill job，例如 ELON_MUSK，不需要每次輸入 platform 或 handle；未提供 earliestAt 時會先向 XTracker 按月往前探測最早可用資料時間。",
            responses = {
                    @ApiResponse(responseCode = "201", description = "建立成功"),
                    @ApiResponse(
                            responseCode = "400",
                            description = "請求參數錯誤",
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
                    )
            }
    )
    @PostMapping("/tracked/{trackedPerson}")
    public ResponseEntity<XTrackerBackfillJobResponse> createTrackedPersonJob(
            @PathVariable XTrackerTrackedPerson trackedPerson,
            @RequestBody(required = false) XTrackerTrackedBackfillJobRequest request
    ) {
        XTrackerBackfillJobRequest jobRequest = new XTrackerBackfillJobRequest(
                trackedPerson.platform(),
                trackedPerson.handle(),
                request == null ? null : request.earliestAt(),
                request == null ? null : request.enableRealtimeAfterBackfill()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(XTrackerBackfillJobResponse.from(backfillJobService.createJob(jobRequest)));
    }
}
