package io.github.yienruuuuu.xtracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.yienruuuuu.common.error.SysCode;
import io.github.yienruuuuu.common.exception.BadRequestApiException;
import io.github.yienruuuuu.xtracker.bean.dto.XTrackerManualSyncResponse;
import io.github.yienruuuuu.xtracker.bean.dto.XTrackerPostData;
import io.github.yienruuuuu.xtracker.bean.dto.XTrackerPostsFetchResult;
import io.github.yienruuuuu.xtracker.bean.dto.XTrackerSyncOptions;
import io.github.yienruuuuu.xtracker.bean.po.XTrackerCrawledPersonEntity;
import io.github.yienruuuuu.xtracker.bean.po.XTrackerCrawledPostEntity;
import io.github.yienruuuuu.xtracker.bean.po.XTrackerRawApiSnapshotEntity;
import io.github.yienruuuuu.xtracker.dao.XTrackerCrawledPersonDao;
import io.github.yienruuuuu.xtracker.dao.XTrackerCrawledPostDao;
import io.github.yienruuuuu.xtracker.dao.XTrackerRawApiSnapshotDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrates one XTracker person posts crawl and persistence cycle.
 */
@Slf4j
@Service
public class XTrackerPersonSyncService {

    private static final String DEFAULT_PLATFORM = "X";
    private static final String PERSON_POSTS_OBJECT_TYPE = "PERSON_POSTS";

    private final XTrackerCrawledPersonDao personDao;
    private final XTrackerCrawledPostDao postDao;
    private final XTrackerRawApiSnapshotDao rawSnapshotDao;
    private final XTrackerFetchService fetchService;
    private final XTrackerPostParseService parseService;
    private final XTrackerHashService hashService;
    private final ObjectMapper objectMapper;

    public XTrackerPersonSyncService(
            XTrackerCrawledPersonDao personDao,
            XTrackerCrawledPostDao postDao,
            XTrackerRawApiSnapshotDao rawSnapshotDao,
            XTrackerFetchService fetchService,
            XTrackerPostParseService parseService,
            XTrackerHashService hashService,
            ObjectMapper objectMapper
    ) {
        this.personDao = personDao;
        this.postDao = postDao;
        this.rawSnapshotDao = rawSnapshotDao;
        this.fetchService = fetchService;
        this.parseService = parseService;
        this.hashService = hashService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public XTrackerManualSyncResponse syncOnce(String platform, String handle, boolean forceRawSnapshot) {
        return syncOnce(platform, handle, new XTrackerSyncOptions(forceRawSnapshot, null, null, null));
    }

    @Transactional
    public XTrackerManualSyncResponse syncOnce(String platform, String handle, XTrackerSyncOptions options) {
        String normalizedPlatform = normalizePlatform(platform);
        String normalizedHandle = normalizeHandle(handle);
        long startedAt = System.currentTimeMillis();
        Instant observedAt = Instant.now();

        log.info("XTracker person sync started. platform={}, handle={}, forceRawSnapshot={}",
                normalizedPlatform, normalizedHandle, options.forceRawSnapshot());

        PersonLookup personLookup = getOrCreatePerson(normalizedPlatform, normalizedHandle);
        XTrackerPostsFetchResult fetchResult = fetchService.fetchPosts(normalizedPlatform, normalizedHandle, options);
        String responseHash = hashService.sha256(fetchResult.rawBody());
        String sourceObjectId = fetchResult.sourceObjectId();

        Optional<XTrackerRawApiSnapshotEntity> latestRaw = rawSnapshotDao
                .findTopBySourceEndpointAndSourceObjectTypeAndSourceObjectIdOrderByFetchedAtDesc(
                        fetchResult.endpoint(),
                        PERSON_POSTS_OBJECT_TYPE,
                        sourceObjectId
                );
        boolean unchanged = latestRaw
                .map(snapshot -> snapshot.getResponseHash().equals(responseHash))
                .orElse(false);
        if (unchanged && !options.forceRawSnapshot()) {
            personLookup.person().setLastSeenAt(observedAt);
            personDao.save(personLookup.person());
            log.info("XTracker person sync skipped unchanged response. platform={}, handle={}, elapsedMs={}",
                    normalizedPlatform, normalizedHandle, System.currentTimeMillis() - startedAt);
            return new XTrackerManualSyncResponse(
                    normalizedPlatform,
                    normalizedHandle,
                    personLookup.created(),
                    false,
                    true,
                    0,
                    0,
                    0,
                    observedAt,
                    System.currentTimeMillis() - startedAt
            );
        }

        XTrackerRawApiSnapshotEntity rawSnapshot = createRawSnapshot(fetchResult, sourceObjectId, responseHash, observedAt);
        rawSnapshotDao.save(rawSnapshot);

        List<XTrackerPostData> posts = parseService.parsePosts(fetchResult.responseBody(), normalizedPlatform, normalizedHandle);
        PostIngestionResult ingestionResult = ingestPosts(personLookup.person(), rawSnapshot, posts);
        personLookup.person().setLastSeenAt(observedAt);
        personDao.save(personLookup.person());

        log.info(
                "XTracker person sync completed. platform={}, handle={}, fetched={}, inserted={}, updated={}, elapsedMs={}",
                normalizedPlatform,
                normalizedHandle,
                posts.size(),
                ingestionResult.inserted(),
                ingestionResult.updated(),
                System.currentTimeMillis() - startedAt
        );

        return new XTrackerManualSyncResponse(
                normalizedPlatform,
                normalizedHandle,
                personLookup.created(),
                true,
                unchanged,
                posts.size(),
                ingestionResult.inserted(),
                ingestionResult.updated(),
                observedAt,
                System.currentTimeMillis() - startedAt
        );
    }

    @Transactional
    public List<XTrackerManualSyncResponse> syncAll(String platform) {
        String normalizedPlatform = normalizePlatform(platform);
        return personDao.findByPlatform(normalizedPlatform).stream()
                .map(person -> syncOnce(normalizedPlatform, person.getHandle(), false))
                .toList();
    }

    private PersonLookup getOrCreatePerson(String platform, String handle) {
        Optional<XTrackerCrawledPersonEntity> existing = personDao.findByPlatformAndHandle(platform, handle);
        if (existing.isPresent()) {
            return new PersonLookup(existing.get(), false);
        }
        XTrackerCrawledPersonEntity person = new XTrackerCrawledPersonEntity();
        person.setPlatform(platform);
        person.setHandle(handle);
        person.setProfileUrl("https://x.com/" + handle);
        return new PersonLookup(personDao.save(person), true);
    }

    private XTrackerRawApiSnapshotEntity createRawSnapshot(
            XTrackerPostsFetchResult fetchResult,
            String sourceObjectId,
            String responseHash,
            Instant observedAt
    ) {
        XTrackerRawApiSnapshotEntity entity = new XTrackerRawApiSnapshotEntity();
        entity.setSourceEndpoint(fetchResult.endpoint());
        entity.setSourceObjectType(PERSON_POSTS_OBJECT_TYPE);
        entity.setSourceObjectId(sourceObjectId);
        entity.setRequestParams(fetchResult.requestParams());
        entity.setHttpStatus(fetchResult.httpStatus());
        entity.setResponseHash(responseHash);
        entity.setResponseBody(fetchResult.responseBody());
        entity.setFetchedAt(observedAt);
        return entity;
    }

    private PostIngestionResult ingestPosts(
            XTrackerCrawledPersonEntity person,
            XTrackerRawApiSnapshotEntity rawSnapshot,
            List<XTrackerPostData> posts
    ) {
        int inserted = 0;
        int updated = 0;
        for (XTrackerPostData post : posts) {
            String contentHash = normalizedPostHash(post);
            Optional<XTrackerCrawledPostEntity> existing = postDao.findByPersonIdAndSourcePostId(
                    person.getId(),
                    post.sourcePostId()
            );
            if (existing.isEmpty()) {
                postDao.save(toEntity(person, rawSnapshot, post, contentHash));
                inserted++;
                continue;
            }
            XTrackerCrawledPostEntity entity = existing.get();
            if (!entity.getContentHash().equals(contentHash)) {
                entity.setPostedAt(post.postedAt());
                entity.setText(post.text());
                entity.setPostUrl(post.postUrl());
                entity.setRawSnapshot(rawSnapshot);
                entity.setContentHash(contentHash);
                postDao.save(entity);
                updated++;
            }
        }
        return new PostIngestionResult(inserted, updated);
    }

    private XTrackerCrawledPostEntity toEntity(
            XTrackerCrawledPersonEntity person,
            XTrackerRawApiSnapshotEntity rawSnapshot,
            XTrackerPostData post,
            String contentHash
    ) {
        XTrackerCrawledPostEntity entity = new XTrackerCrawledPostEntity();
        entity.setPerson(person);
        entity.setSourcePostId(post.sourcePostId());
        entity.setPostedAt(post.postedAt());
        entity.setText(post.text());
        entity.setPostUrl(post.postUrl());
        entity.setRawSnapshot(rawSnapshot);
        entity.setContentHash(contentHash);
        return entity;
    }

    private String normalizedPostHash(XTrackerPostData post) {
        JsonNode rawPost = post.rawPost();
        String source = post.sourcePostId()
                + "|" + post.postedAt()
                + "|" + (post.text() == null ? "" : post.text())
                + "|" + (post.postUrl() == null ? "" : post.postUrl())
                + "|" + (rawPost == null ? "" : rawPost.toString());
        return hashService.sha256(source);
    }

    private String normalizePlatform(String platform) {
        if (platform == null || platform.isBlank()) {
            return DEFAULT_PLATFORM;
        }
        return platform.trim().toUpperCase();
    }

    private String normalizeHandle(String handle) {
        if (handle == null || handle.isBlank()) {
            throw new BadRequestApiException(SysCode.INVALID_ARGUMENT, "XTracker handle is required");
        }
        return handle.trim().replaceFirst("^@", "");
    }

    private record PersonLookup(XTrackerCrawledPersonEntity person, boolean created) {
    }

    private record PostIngestionResult(int inserted, int updated) {
    }
}
