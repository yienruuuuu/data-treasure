package io.github.yienruuuuu.xtracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class XTrackerPersonSyncServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final XTrackerCrawledPersonDao personDao = mock(XTrackerCrawledPersonDao.class);
    private final XTrackerCrawledPostDao postDao = mock(XTrackerCrawledPostDao.class);
    private final XTrackerRawApiSnapshotDao rawSnapshotDao = mock(XTrackerRawApiSnapshotDao.class);
    private final XTrackerFetchService fetchService = mock(XTrackerFetchService.class);
    private final XTrackerPostParseService parseService = mock(XTrackerPostParseService.class);
    private final XTrackerHashService hashService = new XTrackerHashService();
    private final XTrackerPersonSyncService service = new XTrackerPersonSyncService(
            personDao,
            postDao,
            rawSnapshotDao,
            fetchService,
            parseService,
            hashService,
            objectMapper
    );

    @Test
    void syncOnceCreatesPersonRawSnapshotAndPostsOnFirstFetch() throws Exception {
        // given
        when(personDao.findByPlatformAndHandle("X", "elonmusk")).thenReturn(Optional.empty());
        when(personDao.save(any(XTrackerCrawledPersonEntity.class))).thenAnswer(invocation -> {
            XTrackerCrawledPersonEntity person = invocation.getArgument(0);
            if (person.getId() == null) {
                person.setId(UUID.randomUUID());
            }
            return person;
        });
        String rawBody = "{\"posts\":[{\"id\":\"123\"}]}";
        XTrackerPostsFetchResult fetchResult = fetchResult(rawBody);
        when(fetchService.fetchPosts(eq("X"), eq("elonmusk"), any(XTrackerSyncOptions.class))).thenReturn(fetchResult);
        when(rawSnapshotDao.findTopBySourceEndpointAndSourceObjectTypeAndSourceObjectIdOrderByFetchedAtDesc(
                "/users/elonmusk/posts", "PERSON_POSTS", "X:elonmusk:start=:end=:timezone="))
                .thenReturn(Optional.empty());
        when(rawSnapshotDao.save(any(XTrackerRawApiSnapshotEntity.class))).thenAnswer(invocation -> {
            XTrackerRawApiSnapshotEntity raw = invocation.getArgument(0);
            raw.setId(UUID.randomUUID());
            return raw;
        });
        when(parseService.parsePosts(fetchResult.responseBody(), "X", "elonmusk"))
                .thenReturn(List.of(post("123", "hello")));
        when(postDao.findByPersonIdAndPlatformPostId(any(UUID.class), eq("123"))).thenReturn(Optional.empty());
        when(postDao.findByPersonIdAndSourcePostId(any(UUID.class), eq("123"))).thenReturn(Optional.empty());

        // when
        XTrackerManualSyncResponse response = service.syncOnce(null, "@elonmusk", false);

        // then
        assertThat(response.createdPerson()).isTrue();
        assertThat(response.rawSnapshotCreated()).isTrue();
        assertThat(response.postsFetched()).isEqualTo(1);
        assertThat(response.postsInserted()).isEqualTo(1);
        ArgumentCaptor<XTrackerRawApiSnapshotEntity> rawCaptor = ArgumentCaptor.forClass(XTrackerRawApiSnapshotEntity.class);
        verify(rawSnapshotDao).save(rawCaptor.capture());
        assertThat(rawCaptor.getValue().getRequestUrl())
                .isEqualTo("https://xtracker.polymarket.com/api/users/elonmusk/posts?platform=X");
        verify(postDao).save(any(XTrackerCrawledPostEntity.class));
    }

    @Test
    void syncOnceSkipsDatabaseWritesWhenRawResponseIsUnchanged() throws Exception {
        // given
        XTrackerCrawledPersonEntity person = person("X", "elonmusk");
        when(personDao.findByPlatformAndHandle("X", "elonmusk")).thenReturn(Optional.of(person));
        String rawBody = "{\"posts\":[]}";
        XTrackerPostsFetchResult fetchResult = fetchResult(rawBody);
        when(fetchService.fetchPosts(eq("X"), eq("elonmusk"), any(XTrackerSyncOptions.class))).thenReturn(fetchResult);
        XTrackerRawApiSnapshotEntity latest = new XTrackerRawApiSnapshotEntity();
        latest.setResponseHash(hashService.sha256(rawBody));
        when(rawSnapshotDao.findTopBySourceEndpointAndSourceObjectTypeAndSourceObjectIdOrderByFetchedAtDesc(
                "/users/elonmusk/posts", "PERSON_POSTS", "X:elonmusk:start=:end=:timezone="))
                .thenReturn(Optional.of(latest));

        // when
        XTrackerManualSyncResponse response = service.syncOnce("X", "elonmusk", false);

        // then
        assertThat(response.unchanged()).isTrue();
        assertThat(response.rawSnapshotCreated()).isFalse();
        verify(rawSnapshotDao, never()).save(any());
        verify(postDao, never()).save(any());
        verify(parseService, never()).parsePosts(any(), any(), any());
    }

    @Test
    void syncOnceUpdatesExistingPostWhenContentChanges() throws Exception {
        // given
        XTrackerCrawledPersonEntity person = person("X", "elonmusk");
        when(personDao.findByPlatformAndHandle("X", "elonmusk")).thenReturn(Optional.of(person));
        String rawBody = "{\"posts\":[{\"id\":\"123\"}]}";
        XTrackerPostsFetchResult fetchResult = fetchResult(rawBody);
        when(fetchService.fetchPosts(eq("X"), eq("elonmusk"), any(XTrackerSyncOptions.class))).thenReturn(fetchResult);
        when(rawSnapshotDao.findTopBySourceEndpointAndSourceObjectTypeAndSourceObjectIdOrderByFetchedAtDesc(
                "/users/elonmusk/posts", "PERSON_POSTS", "X:elonmusk:start=:end=:timezone="))
                .thenReturn(Optional.empty());
        when(rawSnapshotDao.save(any(XTrackerRawApiSnapshotEntity.class))).thenAnswer(invocation -> {
            XTrackerRawApiSnapshotEntity raw = invocation.getArgument(0);
            raw.setId(UUID.randomUUID());
            return raw;
        });
        when(parseService.parsePosts(fetchResult.responseBody(), "X", "elonmusk"))
                .thenReturn(List.of(post("123", "new text")));
        XTrackerCrawledPostEntity existing = new XTrackerCrawledPostEntity();
        existing.setId(UUID.randomUUID());
        existing.setPerson(person);
        existing.setSourcePostId("123");
        existing.setPostedAt(Instant.parse("2026-05-21T00:00:00Z"));
        existing.setText("old text");
        existing.setContentHash("old-hash");
        when(postDao.findByPersonIdAndPlatformPostId(person.getId(), "123")).thenReturn(Optional.of(existing));

        // when
        XTrackerManualSyncResponse response = service.syncOnce("X", "elonmusk", false);

        // then
        assertThat(response.postsUpdated()).isEqualTo(1);
        ArgumentCaptor<XTrackerCrawledPostEntity> postCaptor = ArgumentCaptor.forClass(XTrackerCrawledPostEntity.class);
        verify(postDao).save(postCaptor.capture());
        assertThat(postCaptor.getValue().getText()).isEqualTo("new text");
    }

    private XTrackerPostsFetchResult fetchResult(String rawBody) throws Exception {
        return new XTrackerPostsFetchResult(
                "/users/elonmusk/posts",
                "https://xtracker.polymarket.com/api/users/elonmusk/posts?platform=X",
                "X",
                "elonmusk",
                "X:elonmusk:start=:end=:timezone=",
                objectMapper.createObjectNode().put("platform", "X"),
                200,
                rawBody,
                objectMapper.readTree(rawBody)
        );
    }

    private XTrackerPostData post(String sourcePostId, String text) throws Exception {
        return new XTrackerPostData(
                sourcePostId,
                "tracker-" + sourcePostId,
                sourcePostId,
                Instant.parse("2026-05-21T00:15:00Z"),
                Instant.parse("2026-05-21T00:16:00Z"),
                text,
                "https://x.com/elonmusk/status/" + sourcePostId,
                objectMapper.readTree("{\"id\":\"" + sourcePostId + "\",\"text\":\"" + text + "\"}")
        );
    }

    private XTrackerCrawledPersonEntity person(String platform, String handle) {
        XTrackerCrawledPersonEntity person = new XTrackerCrawledPersonEntity();
        person.setId(UUID.randomUUID());
        person.setPlatform(platform);
        person.setHandle(handle);
        return person;
    }
}
