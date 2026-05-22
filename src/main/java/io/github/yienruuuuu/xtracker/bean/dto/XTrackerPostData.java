package io.github.yienruuuuu.xtracker.bean.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record XTrackerPostData(
        String sourcePostId,
        Instant postedAt,
        String text,
        String postUrl,
        JsonNode rawPost
) {
}
