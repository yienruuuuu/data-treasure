package io.github.yienruuuuu.xtracker.bean.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;

public record XTrackerSyncOptions(
        boolean forceRawSnapshot,
        Instant startDate,
        Instant endDate,
        String timezone
) {

    public ObjectNode toRequestParams(ObjectMapper objectMapper, String platform) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("platform", platform);
        if (startDate != null) {
            node.put("startDate", startDate.toString());
        }
        if (endDate != null) {
            node.put("endDate", endDate.toString());
        }
        if (shouldForwardTimezone()) {
            node.put("timezone", timezone.trim());
        }
        return node;
    }

    public String sourceObjectId(String platform, String handle) {
        return platform + ":" + handle
                + ":start=" + (startDate == null ? "" : startDate)
                + ":end=" + (endDate == null ? "" : endDate)
                + ":timezone=" + (shouldForwardTimezone() ? timezone.trim() : "");
    }

    public boolean shouldForwardTimezone() {
        return startDate == null && endDate == null && timezone != null && !timezone.isBlank();
    }
}
