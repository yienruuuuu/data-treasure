package io.github.yienruuuuu.xtracker.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.yienruuuuu.common.bean.dto.ApiErrorResponse;
import io.github.yienruuuuu.common.error.SysCode;
import io.github.yienruuuuu.common.exception.InternalApiException;
import io.github.yienruuuuu.xtracker.bean.dto.XTrackerActivityTrendResponse;
import io.github.yienruuuuu.xtracker.bean.dto.XTrackerManualSyncRequest;
import io.github.yienruuuuu.xtracker.bean.dto.XTrackerManualSyncResponse;
import io.github.yienruuuuu.xtracker.bean.dto.XTrackerPostCountResponse;
import io.github.yienruuuuu.xtracker.bean.dto.XTrackerSyncOptions;
import io.github.yienruuuuu.xtracker.domain.XTrackerTrackedPerson;
import io.github.yienruuuuu.xtracker.service.XTrackerPostQueryService;
import io.github.yienruuuuu.xtracker.service.XTrackerPersonSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Manual endpoints for XTracker person crawler operations.
 */
@Tag(name = "XTracker 人物爬蟲", description = "提供人工觸發 XTracker 人物發文資料蒐集的 API。")
@Slf4j
@RestController
@RequestMapping("/api/xtracker/persons")
public class XTrackerPersonController {

    private static final String PUBLIC_ACTIVITY_TREND_CACHE_CONTROL = "public, max-age=60, stale-while-revalidate=240";

    private final XTrackerPersonSyncService syncService;
    private final XTrackerPostQueryService queryService;
    private final ObjectMapper objectMapper;

    public XTrackerPersonController(
            XTrackerPersonSyncService syncService,
            XTrackerPostQueryService queryService,
            ObjectMapper objectMapper
    ) {
        this.syncService = syncService;
        this.queryService = queryService;
        this.objectMapper = objectMapper;
    }

    @Operation(
            summary = "人工同步一次 XTracker 人物發文",
            description = "立即抓取指定人物的 XTracker posts API，保存 raw JSONB 並 upsert 逐筆發文資料，可用於測試與人工調校。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "同步成功"),
                    @ApiResponse(responseCode = "400", description = "參數錯誤",
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "同步失敗",
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    @PostMapping("/{handle}/sync-once")
    public XTrackerManualSyncResponse syncOnce(
            @PathVariable String handle,
            @RequestBody(required = false) XTrackerManualSyncRequest request
    ) {
        String platform = request == null ? null : request.platform();
        XTrackerSyncOptions options = toOptions(request);
        log.info("Manual XTracker sync requested. platform={}, handle={}, forceRawSnapshot={}",
                platform, handle, options.forceRawSnapshot());
        return syncService.syncOnce(platform, handle, options);
    }

    @Operation(
            summary = "人工同步一次預設 XTracker 人物",
            description = "使用 enum 管理的追蹤人物執行同步，例如 ELON_MUSK，不需要每次輸入 platform 或 handle。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "同步成功"),
                    @ApiResponse(responseCode = "400", description = "參數錯誤",
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "同步失敗",
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    @PostMapping("/tracked/{trackedPerson}/sync-once")
    public XTrackerManualSyncResponse syncTrackedPersonOnce(
            @PathVariable XTrackerTrackedPerson trackedPerson,
            @RequestBody(required = false) XTrackerManualSyncRequest request
    ) {
        XTrackerSyncOptions options = toOptions(request);
        log.info("Manual XTracker tracked person sync requested. trackedPerson={}, forceRawSnapshot={}",
                trackedPerson, options.forceRawSnapshot());
        return syncService.syncOnce(trackedPerson.platform(), trackedPerson.handle(), options);
    }

    @Operation(
            summary = "查詢人物在指定時間窗的發文數",
            description = "依逐筆發文表的 posted_at 區間計算發文數，例如查詢 Elon Musk 在 2026-05-21 08:00 至 09:00 的發文數。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查詢成功"),
                    @ApiResponse(responseCode = "400", description = "參數錯誤",
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "找不到人物",
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    @GetMapping("/{handle}/posts/count")
    public XTrackerPostCountResponse countPosts(
            @PathVariable String handle,
            @RequestParam(required = false, defaultValue = "X") String platform,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.Instant startAt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.Instant endAt
    ) {
        return queryService.countPosts(platform, handle, startAt, endAt);
    }

    @Operation(
            summary = "查詢預設人物在指定時間窗的發文數",
            description = "使用 enum 管理的追蹤人物查詢發文數，例如 ELON_MUSK。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查詢成功"),
                    @ApiResponse(responseCode = "400", description = "參數錯誤",
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "找不到人物",
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    @GetMapping("/tracked/{trackedPerson}/posts/count")
    public XTrackerPostCountResponse countTrackedPersonPosts(
            @PathVariable XTrackerTrackedPerson trackedPerson,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.Instant startAt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.Instant endAt
    ) {
        return queryService.countPosts(trackedPerson.platform(), trackedPerson.handle(), startAt, endAt);
    }

    @Operation(
            summary = "查詢預設人物發文量與累積趨勢",
            description = "提供公開 PolyTracker Current 圖表使用。Request/response 時間皆為 UTC Instant，前端瀏覽器負責轉成美東時間顯示。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查詢成功"),
                    @ApiResponse(responseCode = "304", description = "資料未變更"),
                    @ApiResponse(responseCode = "400", description = "參數錯誤",
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "找不到人物",
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    @GetMapping("/tracked/{trackedPerson}/posts/activity-trend")
    public ResponseEntity<XTrackerActivityTrendResponse> getTrackedPersonActivityTrend(
            @PathVariable XTrackerTrackedPerson trackedPerson,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endAt,
            @RequestParam(required = false) String bucket,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch
    ) {
        XTrackerActivityTrendResponse response = queryService.getActivityTrend(trackedPerson, startAt, endAt, bucket);
        String etag = etag(response);
        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .header(HttpHeaders.CACHE_CONTROL, PUBLIC_ACTIVITY_TREND_CACHE_CONTROL)
                    .header(HttpHeaders.ETAG, etag)
                    .build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, PUBLIC_ACTIVITY_TREND_CACHE_CONTROL)
                .header(HttpHeaders.ETAG, etag)
                .body(response);
    }

    private XTrackerSyncOptions toOptions(XTrackerManualSyncRequest request) {
        if (request == null) {
            return new XTrackerSyncOptions(false, null, null, null);
        }
        return new XTrackerSyncOptions(
                Boolean.TRUE.equals(request.forceRawSnapshot()),
                request.startDate(),
                request.endDate(),
                request.timezone()
        );
    }

    private String etag(XTrackerActivityTrendResponse response) {
        try {
            byte[] payload = objectMapper.writeValueAsBytes(response);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return "\"" + HexFormat.of().formatHex(digest.digest(payload)) + "\"";
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new InternalApiException(SysCode.INTERNAL_ERROR, "Failed to calculate activity trend ETag", exception);
        }
    }
}
