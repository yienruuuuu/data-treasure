package io.github.yienruuuuu.xtracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.yienruuuuu.xtracker.bean.dto.XTrackerPostData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Parses XTracker post responses while keeping source-shape assumptions narrow.
 */
@Slf4j
@Service
public class XTrackerPostParseService {

    public List<XTrackerPostData> parsePosts(JsonNode responseBody, String platform, String handle) {
        JsonNode postsNode = locatePostsArray(responseBody);
        if (postsNode == null || !postsNode.isArray()) {
            log.warn("XTracker posts response does not contain a post array. platform={}, handle={}", platform, handle);
            return List.of();
        }

        List<XTrackerPostData> posts = new ArrayList<>();
        for (JsonNode node : postsNode) {
            XTrackerPostData post = parsePost(node, handle);
            if (post != null) {
                posts.add(post);
            }
        }
        return posts;
    }

    private JsonNode locatePostsArray(JsonNode root) {
        if (root == null || root.isNull()) {
            return null;
        }
        if (root.isArray()) {
            return root;
        }
        for (String field : List.of("posts", "data", "items", "results")) {
            JsonNode candidate = root.path(field);
            if (candidate.isArray()) {
                return candidate;
            }
            JsonNode nestedPosts = candidate.path("posts");
            if (nestedPosts.isArray()) {
                return nestedPosts;
            }
        }
        Iterator<JsonNode> values = root.elements();
        while (values.hasNext()) {
            JsonNode value = values.next();
            if (value.isArray()) {
                return value;
            }
        }
        return null;
    }

    private XTrackerPostData parsePost(JsonNode node, String handle) {
        String trackerPostId = firstText(node, "id");
        String platformPostId = firstText(node, "platformId", "postId", "post_id", "tweetId", "tweet_id", "sourcePostId");
        String sourcePostId = platformPostId == null ? trackerPostId : platformPostId;
        Instant postedAt = firstInstant(node, "postedAt", "posted_at", "createdAt", "created_at", "timestamp", "time", "date");
        Instant importedAt = firstInstant(node, "importedAt", "imported_at");
        if (sourcePostId == null || postedAt == null) {
            log.debug("Skip XTracker post without id or posted time. id={}, postedAt={}", sourcePostId, postedAt);
            return null;
        }
        String text = firstText(node, "text", "content", "fullText", "full_text", "body");
        String postUrl = firstText(node, "url", "postUrl", "post_url", "link");
        if (postUrl == null || postUrl.isBlank()) {
            postUrl = "https://x.com/" + handle + "/status/" + sourcePostId;
        }
        return new XTrackerPostData(sourcePostId, trackerPostId, platformPostId, postedAt, importedAt, text, postUrl, node);
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (value.isTextual() && !value.asText().isBlank()) {
                return value.asText();
            }
            if (value.isNumber()) {
                return value.asText();
            }
        }
        return null;
    }

    private Instant firstInstant(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.path(field);
            Instant parsed = parseInstant(value);
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private Instant parseInstant(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            long epoch = value.asLong();
            return epoch > 10_000_000_000L ? Instant.ofEpochMilli(epoch) : Instant.ofEpochSecond(epoch);
        }
        if (!value.isTextual() || value.asText().isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value.asText());
        } catch (DateTimeParseException exception) {
            return null;
        }
    }
}
