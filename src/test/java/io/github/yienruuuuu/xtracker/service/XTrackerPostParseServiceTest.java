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
                      "createdAt": "2026-05-21T00:15:00Z",
                      "text": "hello",
                      "url": "https://x.com/elonmusk/status/123"
                    }
                  ]
                }
                """);

        // when
        List<XTrackerPostData> posts = service.parsePosts(response, "X", "elonmusk");

        // then
        assertThat(posts).hasSize(1);
        assertThat(posts.get(0).sourcePostId()).isEqualTo("123");
        assertThat(posts.get(0).postedAt()).isEqualTo(Instant.parse("2026-05-21T00:15:00Z"));
        assertThat(posts.get(0).text()).isEqualTo("hello");
        assertThat(posts.get(0).postUrl()).isEqualTo("https://x.com/elonmusk/status/123");
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
