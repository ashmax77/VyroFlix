package com.vyroflix.common.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson {@link ObjectMapper} configuration shared across all services.
 *
 * <ul>
 *   <li>Java 8+ date/time serialized as ISO 8601 strings, not numeric timestamps.</li>
 *   <li>Unknown JSON properties are ignored to allow forward-compatible events.</li>
 *   <li>Empty beans are serialized without error.</li>
 * </ul>
 */
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Java 8 date/time support
        mapper.registerModule(new JavaTimeModule());

        // ISO 8601 strings, not epoch millis
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Forward compatibility: ignore unknown fields in event payloads
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // Prevent failure on empty beans
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        return mapper;
    }
}
