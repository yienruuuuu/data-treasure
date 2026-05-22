package io.github.yienruuuuu.xtracker.bean.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record XTrackerPostsFetchResult(
        String endpoint,
        String platform,
        String handle,
        String sourceObjectId,
        JsonNode requestParams,
        int httpStatus,
        String rawBody,
        JsonNode responseBody
) {
}
