package io.github.yienruuuuu.xtracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.yienruuuuu.xtracker.bean.dto.XTrackerPostData;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class XTrackerPostParseServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final XTrackerPostParseService service = new XTrackerPostParseService();

    @Test
    void parsePostsExtractsPostMetadataFromWrappedResponse() throws Exception {
        // given
        JsonNode response = objectMapper.readTree("""
                {
                  "posts": [
                    {
                      "id": "123",
                      "platformId": "456",
                      "createdAt": "2026-05-21T00:15:00Z",
                      "importedAt": "2026-05-21T00:16:00Z",
                      "text": "hello",
                      "url": "https://x.com/elonmusk/status/456"
                    }
                  ]
                }
                """);

        // when
        List<XTrackerPostData> posts = service.parsePosts(response, "X", "elonmusk");

        // then
        assertThat(posts).hasSize(1);
        assertThat(posts.get(0).sourcePostId()).isEqualTo("456");
        assertThat(posts.get(0).trackerPostId()).isEqualTo("123");
        assertThat(posts.get(0).platformPostId()).isEqualTo("456");
        assertThat(posts.get(0).postedAt()).isEqualTo(Instant.parse("2026-05-21T00:15:00Z"));
        assertThat(posts.get(0).importedAt()).isEqualTo(Instant.parse("2026-05-21T00:16:00Z"));
        assertThat(posts.get(0).text()).isEqualTo("hello");
        assertThat(posts.get(0).postUrl()).isEqualTo("https://x.com/elonmusk/status/456");
    }

    @Test
    void parsePostsBuildsXUrlFromPlatformIdWhenUrlIsMissing() throws Exception {
        // given
        JsonNode response = objectMapper.readTree("""
                {
                  "success": true,
                  "data": [
                    {
                      "id": "cmpgmjwxi0006ic04lt27s8ct",
                      "platformId": "2057730745821483416",
                      "createdAt": "2026-05-22T07:50:15.000Z",
                      "content": "hello"
                    }
                  ]
                }
                """);

        // when
        List<XTrackerPostData> posts = service.parsePosts(response, "X", "elonmusk");

        // then
        assertThat(posts).hasSize(1);
        assertThat(posts.get(0).sourcePostId()).isEqualTo("2057730745821483416");
        assertThat(posts.get(0).postUrl()).isEqualTo("https://x.com/elonmusk/status/2057730745821483416");
    }

    @Test
    void parsePostsSkipsRowsWithoutRequiredIdOrTime() throws Exception {
        // given
        JsonNode response = objectMapper.readTree("""
                [
                  {"id": "123", "text": "missing time"},
                  {"createdAt": "2026-05-21T00:15:00Z", "text": "missing id"}
                ]
                """);

        // when
        List<XTrackerPostData> posts = service.parsePosts(response, "X", "elonmusk");

        // then
        assertThat(posts).isEmpty();
    }
}
