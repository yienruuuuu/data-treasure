package io.github.yienruuuuu.xtracker.service;

import io.github.yienruuuuu.common.exception.BadRequestApiException;
import io.github.yienruuuuu.common.exception.NotFoundApiException;
import io.github.yienruuuuu.xtracker.bean.dto.XTrackerPostCountResponse;
import io.github.yienruuuuu.xtracker.bean.po.XTrackerCrawledPersonEntity;
import io.github.yienruuuuu.xtracker.dao.XTrackerCrawledPersonDao;
import io.github.yienruuuuu.xtracker.dao.XTrackerCrawledPostDao;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class XTrackerPostQueryServiceTest {

    private final XTrackerCrawledPersonDao personDao = mock(XTrackerCrawledPersonDao.class);
    private final XTrackerCrawledPostDao postDao = mock(XTrackerCrawledPostDao.class);
    private final XTrackerPostQueryService service = new XTrackerPostQueryService(personDao, postDao);

    @Test
    void countPostsReturnsRangeCountForPerson() {
        // given
        XTrackerCrawledPersonEntity person = new XTrackerCrawledPersonEntity();
        person.setId(UUID.randomUUID());
        person.setPlatform("X");
        person.setHandle("elonmusk");
        Instant startAt = Instant.parse("2026-05-21T00:00:00Z");
        Instant endAt = Instant.parse("2026-05-21T01:00:00Z");
        when(personDao.findByPlatformAndHandle("X", "elonmusk")).thenReturn(Optional.of(person));
        when(postDao.countByPersonIdAndPostedAtRange(person.getId(), startAt, endAt)).thenReturn(7L);

        // when
        XTrackerPostCountResponse response = service.countPosts("x", "@elonmusk", startAt, endAt);

        // then
        assertThat(response.postCount()).isEqualTo(7);
        assertThat(response.startAt()).isEqualTo(startAt);
        assertThat(response.endAt()).isEqualTo(endAt);
    }

    @Test
    void countPostsRejectsInvalidRange() {
        // given
        Instant startAt = Instant.parse("2026-05-21T01:00:00Z");
        Instant endAt = Instant.parse("2026-05-21T00:00:00Z");

        // when / then
        assertThatThrownBy(() -> service.countPosts("X", "elonmusk", startAt, endAt))
                .isInstanceOf(BadRequestApiException.class)
                .hasMessageContaining("startAt");
    }

    @Test
    void countPostsThrowsWhenPersonDoesNotExist() {
        // given
        Instant startAt = Instant.parse("2026-05-21T00:00:00Z");
        Instant endAt = Instant.parse("2026-05-21T01:00:00Z");
        when(personDao.findByPlatformAndHandle("X", "elonmusk")).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> service.countPosts("X", "elonmusk", startAt, endAt))
                .isInstanceOf(NotFoundApiException.class)
                .hasMessageContaining("not found");
    }
}
