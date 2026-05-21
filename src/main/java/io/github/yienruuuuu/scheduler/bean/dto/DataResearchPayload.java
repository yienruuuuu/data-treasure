package io.github.yienruuuuu.scheduler.bean.dto;

public record DataResearchPayload(
        String subject,
        String depth,
        boolean fail
) {
    public String normalizedSubject() {
        return subject == null || subject.isBlank() ? "unknown" : subject.trim();
    }

    public String normalizedDepth() {
        return depth == null || depth.isBlank() ? "basic" : depth.trim();
    }
}
