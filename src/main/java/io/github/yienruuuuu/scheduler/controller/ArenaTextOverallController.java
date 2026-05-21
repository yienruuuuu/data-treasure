package io.github.yienruuuuu.scheduler.controller;

import io.github.yienruuuuu.common.bean.dto.ApiErrorResponse;
import io.github.yienruuuuu.scheduler.bean.dto.ArenaTextOverallManualSyncRequest;
import io.github.yienruuuuu.scheduler.bean.dto.ArenaTextOverallManualSyncResponse;
import io.github.yienruuuuu.scheduler.bean.dto.ArenaTextOverallSnapshotData;
import io.github.yienruuuuu.scheduler.service.ArenaTextOverallSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Manual endpoints for arena text-overall synchronization.
 */
@Tag(name = "Arena 榜單同步", description = "提供人工觸發 Arena Text Overall 同步的 API。")
@Slf4j
@RestController
@RequestMapping("/api/arena-text-overall")
public class ArenaTextOverallController {

    private final ArenaTextOverallSyncService arenaTextOverallSyncService;

    public ArenaTextOverallController(ArenaTextOverallSyncService arenaTextOverallSyncService) {
        this.arenaTextOverallSyncService = arenaTextOverallSyncService;
    }

    @Operation(
            summary = "人工同步一次 Arena Text Overall",
            description = "立即抓取排行榜、解析資料並寫入 snapshot/item 表，可用於人工補跑。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "同步成功"),
                    @ApiResponse(responseCode = "400", description = "參數錯誤或來源資料格式不符",
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "同步失敗",
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    @PostMapping("/sync-once")
    public ArenaTextOverallManualSyncResponse syncOnce(
            @RequestBody(required = false) ArenaTextOverallManualSyncRequest request
    ) {
        String sourceUrl = request == null ? null : request.sourceUrl();
        log.info("Manual arena sync requested. sourceUrl={}", sourceUrl);
        ArenaTextOverallSnapshotData snapshotData = arenaTextOverallSyncService.syncOnce(sourceUrl);
        log.info("Manual arena sync finished. leaderboardKey={}, updatedDate={}, fetched={}",
                snapshotData.leaderboardKey(), snapshotData.updatedDate(), snapshotData.items().size());
        return ArenaTextOverallManualSyncResponse.from(snapshotData);
    }
}
