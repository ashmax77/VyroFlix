package com.vyroflix.common.messaging.upstash;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyroflix.common.messaging.EventPublisher;
import com.vyroflix.common.messaging.MessagingProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

/**
 * Spring configuration for Upstash Kafka REST publishing.
 *
 * <p>Activated when {@code vyroflix.messaging.type=upstash}.</p>
 */
@Configuration
@EnableConfigurationProperties(MessagingProperties.class)
@ConditionalOnProperty(prefix = "vyroflix.messaging", name = "type", havingValue = "upstash")
public class UpstashConfig {

    @Bean
    @ConditionalOnMissingBean(name = "upstashRestClient")
    public RestClient upstashRestClient(MessagingProperties properties) {
        String baseUrl = properties.getUpstash().getRestUrl();
        String token = properties.getUpstash().getRestToken();

        RestClient.Builder builder = RestClient.builder();
        if (baseUrl != null && !baseUrl.isBlank()) {
            builder.baseUrl(baseUrl);
        }
        if (token != null && !token.isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean(EventPublisher.class)
    public EventPublisher upstashEventPublisher(RestClient upstashRestClient, ObjectMapper objectMapper) {
        return new UpstashEventPublisher(upstashRestClient, objectMapper);
    }
}
