package io.github.yienruuuuu.scheduler.service;

import io.github.yienruuuuu.scheduler.bean.dto.ArenaTextOverallSnapshotData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Orchestrates one-shot arena text-overall synchronization.
 */
@Slf4j
@Service
public class ArenaTextOverallSyncService {

    private static final String DEFAULT_SOURCE_URL = "https://arena.ai/leaderboard/text";

    private final ArenaTextOverallFetchService fetchService;
    private final ArenaTextOverallParseService parseService;
    private final ArenaTextOverallIngestionService ingestionService;

    public ArenaTextOverallSyncService(
            ArenaTextOverallFetchService fetchService,
            ArenaTextOverallParseService parseService,
            ArenaTextOverallIngestionService ingestionService
    ) {
        this.fetchService = fetchService;
        this.parseService = parseService;
        this.ingestionService = ingestionService;
    }

    public ArenaTextOverallSnapshotData syncOnce(String sourceUrl) {
        String effectiveSourceUrl = sourceUrl == null || sourceUrl.isBlank()
                ? DEFAULT_SOURCE_URL
                : sourceUrl.trim();
        long startedAt = System.currentTimeMillis();
        log.info("Arena sync started. sourceUrl={}", effectiveSourceUrl);
        String html = fetchService.fetchHtml(effectiveSourceUrl);
        ArenaTextOverallSnapshotData parsed = parseService.parse(effectiveSourceUrl, html);
        ingestionService.ingest(parsed);
        log.info("Arena sync completed. sourceUrl={}, updatedDate={}, declared={}, fetched={}, elapsedMs={}",
                effectiveSourceUrl,
                parsed.updatedDate(),
                parsed.declaredModelCount(),
                parsed.items().size(),
                System.currentTimeMillis() - startedAt);
        return parsed;
    }
}
