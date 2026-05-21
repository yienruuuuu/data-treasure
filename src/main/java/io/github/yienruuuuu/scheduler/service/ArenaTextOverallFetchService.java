package io.github.yienruuuuu.scheduler.service;

import org.springframework.http.ResponseEntity;
import org.springframework.boot.web.client.RestTemplateBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Fetches Arena leaderboard HTML for downstream parsing.
 */
@Slf4j
@Service
public class ArenaTextOverallFetchService {

    private final RestTemplate restTemplate;

    public ArenaTextOverallFetchService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    public String fetchHtml(String sourceUrl) {
        log.debug("Fetching arena leaderboard HTML. sourceUrl={}", sourceUrl);
        ResponseEntity<String> response = restTemplate.getForEntity(sourceUrl, String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("Failed to fetch arena leaderboard page: " + sourceUrl);
        }
        log.debug("Fetched arena leaderboard HTML. sourceUrl={}, htmlLength={}", sourceUrl, response.getBody().length());
        return response.getBody();
    }
}
