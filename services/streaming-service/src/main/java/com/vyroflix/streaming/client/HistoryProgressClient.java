package com.vyroflix.streaming.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.UUID;

/**
 * Synchronous lookup of the viewer's saved position from History-Service.
 *
 * <p>Fail-open: any failure (timeout, history down, no saved progress) resolves to 0 so playback is never
 * blocked. The caller's bearer token is forwarded so History enforces ownership; it is never logged.</p>
 */
@Slf4j
@Component
public class HistoryProgressClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(3);

    private final RestClient restClient;

    public HistoryProgressClient(@Value("${vyroflix.services.history-url:http://localhost:8086}") String historyBaseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        this.restClient = RestClient.builder()
                .baseUrl(historyBaseUrl)
                .requestFactory(factory)
                .build();
    }

    public int resumePositionSeconds(UUID contentId, String authorizationHeader) {
        if (contentId == null || authorizationHeader == null || authorizationHeader.isBlank()) {
            return 0;
        }
        try {
            ProgressView view = restClient.get()
                    .uri("/api/v1/history/progress/{contentId}", contentId)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .retrieve()
                    .body(ProgressView.class);
            if (view == null || view.isCompleted()) {
                return 0;
            }
            return Math.max(0, view.getPositionSeconds());
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() != HttpStatus.NOT_FOUND) {
                log.warn("History progress lookup rejected with status {}", e.getStatusCode().value());
            }
            return 0;
        } catch (RestClientException e) {
            log.warn("History progress lookup failed; starting from 0: {}", e.getClass().getSimpleName());
            return 0;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ProgressView {
        private int positionSeconds;
        private boolean completed;
    }
}
