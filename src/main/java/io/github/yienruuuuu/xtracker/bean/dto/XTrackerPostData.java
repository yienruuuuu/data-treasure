package io.github.yienruuuuu.xtracker.bean.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record XTrackerPostData(
        String sourcePostId,
        String trackerPostId,
        String platformPostId,
        Instant postedAt,
        Instant importedAt,
        String text,
        String postUrl,
        JsonNode rawPost
) {
}
