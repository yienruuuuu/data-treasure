package io.github.yienruuuuu.scheduler.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.yienruuuuu.scheduler.bean.dto.ArenaTextOverallSnapshotData;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArenaTextOverallParseServiceTest {

    private final ArenaTextOverallParseService service = new ArenaTextOverallParseService(new ObjectMapper());

    @Test
    void parseExtractsHeaderAndRowsFromTextFallback() {
        String html = """
                <html>
                <body>
                Text Arena Overall
                May 18, 2026
                6,297,180 votes
                360 models
                1
                1 4
                claude-opus-4-6-thinking
                Anthropic · Proprietary
                1502±4
                27,454 $5 / $25
                1M
                </body>
                </html>
                """;

        ArenaTextOverallSnapshotData parsed = service.parse("https://arena.ai/leaderboard/text", html);

        assertThat(parsed.updatedDate().toString()).isEqualTo("2026-05-18");
        assertThat(parsed.totalVotes()).isEqualTo(6_297_180L);
        assertThat(parsed.declaredModelCount()).isEqualTo(360);
        assertThat(parsed.items()).hasSize(1);
        assertThat(parsed.items().get(0).rank()).isEqualTo(1);
        assertThat(parsed.items().get(0).modelName()).isEqualTo("claude-opus-4-6-thinking");
    }

    @Test
    void parseExtractsRowsFromEmbeddedJsonScript() {
        String html = """
                <html>
                <body>
                May 18, 2026
                6,297,180 votes
                360 models
                <script>
                window.__NEXT_DATA__ = {"props":{"pageProps":{"rows":[{"rank":1,"score":1502,"model":"claude-opus-4-6-thinking","provider":"Anthropic","license_type":"Proprietary","score_ci":4}]}}};
                </script>
                </body>
                </html>
                """;

        ArenaTextOverallSnapshotData parsed = service.parse("https://arena.ai/leaderboard/text", html);

        assertThat(parsed.items()).hasSize(1);
        assertThat(parsed.items().get(0).rank()).isEqualTo(1);
        assertThat(parsed.items().get(0).score()).isEqualTo(1502);
        assertThat(parsed.items().get(0).modelName()).isEqualTo("claude-opus-4-6-thinking");
    }

    @Test
    void parseExtractsRowsFromOneLineTableMarkup() {
        String html = """
                <html><body>May 18, 2026 6,297,180 votes 360 models
                <table><tbody>
                <tr><td><span>1</span></td><td><span>1</span><span>4</span></td>
                <td><a href="https://example.com/model-1" title="claude-opus-4-6-thinking">claude-opus-4-6-thinking</a><span>Anthropic · Proprietary</span></td>
                <td><span>1502</span><span>±4</span></td><td><span>27,454</span></td><td><span>$5 / $25</span></td><td><span>1M</span></td></tr>
                </tbody></table></body></html>
                """;

        ArenaTextOverallSnapshotData parsed = service.parse("https://arena.ai/leaderboard/text", html);

        assertThat(parsed.items()).hasSize(1);
        assertThat(parsed.items().get(0).rank()).isEqualTo(1);
        assertThat(parsed.items().get(0).modelName()).isEqualTo("claude-opus-4-6-thinking");
        assertThat(parsed.items().get(0).providerName()).isEqualTo("Anthropic");
        assertThat(parsed.items().get(0).licenseType()).isEqualTo("Proprietary");
        assertThat(parsed.items().get(0).score()).isEqualTo(1502);
    }
}
