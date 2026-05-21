package io.github.yienruuuuu.scheduler.service;

import io.github.yienruuuuu.common.error.SysCode;
import io.github.yienruuuuu.common.exception.InternalApiException;
import io.github.yienruuuuu.scheduler.bean.dto.ArenaTextOverallItemData;
import io.github.yienruuuuu.scheduler.bean.dto.ArenaTextOverallSnapshotData;
import io.github.yienruuuuu.scheduler.bean.po.ArenaTextOverallItemEntity;
import io.github.yienruuuuu.scheduler.bean.po.ArenaTextOverallSnapshotEntity;
import io.github.yienruuuuu.scheduler.dao.ArenaTextOverallItemDao;
import io.github.yienruuuuu.scheduler.dao.ArenaTextOverallSnapshotDao;
import io.github.yienruuuuu.scheduler.domain.ArenaCrawlStatus;
import io.github.yienruuuuu.telegram.service.TelegramNotifyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Persists arena text-overall snapshots and ranking rows.
 */
@Slf4j
@Service
public class ArenaTextOverallIngestionService {

    private final ArenaTextOverallSnapshotDao snapshotDao;
    private final ArenaTextOverallItemDao itemDao;
    private final TelegramNotifyService telegramNotifyService;

    public ArenaTextOverallIngestionService(
            ArenaTextOverallSnapshotDao snapshotDao,
            ArenaTextOverallItemDao itemDao,
            TelegramNotifyService telegramNotifyService
    ) {
        this.snapshotDao = snapshotDao;
        this.itemDao = itemDao;
        this.telegramNotifyService = telegramNotifyService;
    }

    @Transactional
    public void ingest(ArenaTextOverallSnapshotData parsed) {
        boolean exists = snapshotDao.findByLeaderboardKeyAndUpdatedDate(
                parsed.leaderboardKey(),
                parsed.updatedDate()
        ).isPresent();
        if (exists) {
            log.info("Arena snapshot already exists. leaderboardKey={}, updatedDate={}",
                    parsed.leaderboardKey(), parsed.updatedDate());
            return;
        }

        int fetchedCount = parsed.items().size();
        if (fetchedCount == 0) {
            throw new InternalApiException(
                    SysCode.INTERNAL_ERROR,
                    "Arena crawl returned zero items, skip persistence to avoid empty snapshot",
                    null
            );
        }
        ArenaCrawlStatus status = fetchedCount < parsed.declaredModelCount()
                ? ArenaCrawlStatus.PARTIAL
                : ArenaCrawlStatus.SUCCESS;

        ArenaTextOverallSnapshotEntity snapshot = new ArenaTextOverallSnapshotEntity();
        snapshot.setLeaderboardKey(parsed.leaderboardKey());
        snapshot.setSourceUrl(parsed.sourceUrl());
        snapshot.setUpdatedDate(parsed.updatedDate());
        snapshot.setTotalVotes(parsed.totalVotes());
        snapshot.setDeclaredModelCount(parsed.declaredModelCount());
        snapshot.setFetchedModelCount(fetchedCount);
        snapshot.setCrawlStatus(status);
        snapshot.setFetchedAt(Instant.now());
        snapshotDao.save(snapshot);

        List<ArenaTextOverallItemEntity> items = parsed.items().stream()
                .map(data -> toEntity(snapshot, data))
                .toList();
        itemDao.saveAll(items);

        if (status == ArenaCrawlStatus.PARTIAL) {
            handleCrawlAnomaly(new CrawlAnomalyContext(
                    parsed.leaderboardKey(),
                    parsed.sourceUrl(),
                    parsed.updatedDate().toString(),
                    parsed.declaredModelCount(),
                    fetchedCount
            ));
        }
    }

    protected void handleCrawlAnomaly(CrawlAnomalyContext context) {
        log.warn(
                "Arena crawl partial result. leaderboardKey={}, sourceUrl={}, updatedDate={}, declared={}, fetched={}",
                context.leaderboardKey(),
                context.sourceUrl(),
                context.updatedDate(),
                context.declaredModelCount(),
                context.fetchedModelCount()
        );
        try {
            telegramNotifyService.sendCrawlAnomaly(context);
        } catch (Exception exception) {
            log.error("Failed to send crawl anomaly Telegram notification", exception);
        }
    }

    private ArenaTextOverallItemEntity toEntity(ArenaTextOverallSnapshotEntity snapshot, ArenaTextOverallItemData data) {
        ArenaTextOverallItemEntity entity = new ArenaTextOverallItemEntity();
        entity.setSnapshot(snapshot);
        entity.setRank(data.rank());
        entity.setRankSpreadMin(data.rankSpreadMin());
        entity.setRankSpreadMax(data.rankSpreadMax());
        entity.setModelName(data.modelName());
        entity.setProviderName(data.providerName());
        entity.setLicenseType(data.licenseType());
        entity.setScore(data.score());
        entity.setScoreCi(data.scoreCi());
        entity.setModelVotes(data.modelVotes());
        entity.setInputPricePerM(data.inputPricePerM());
        entity.setOutputPricePerM(data.outputPricePerM());
        entity.setContextLengthText(data.contextLengthText());
        entity.setPreliminary(data.preliminary());
        entity.setModelUrl(data.modelUrl());
        return entity;
    }

    public record CrawlAnomalyContext(
            String leaderboardKey,
            String sourceUrl,
            String updatedDate,
            int declaredModelCount,
            int fetchedModelCount
    ) {
    }
}
