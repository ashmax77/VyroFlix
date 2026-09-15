package com.vyroflix.common.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;

/**
 * Jackson configuration shared across all services.
 *
 * <ul>
 *   <li>Java 8+ date/time serialized as ISO 8601 strings, not numeric timestamps.</li>
 *   <li>Unknown JSON properties are ignored to allow forward-compatible events.</li>
 *   <li>Empty beans are serialized without error.</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnClass(ObjectMapper.class)
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer vyroflixJacksonCustomizer() {
        return builder -> {
            builder.modules(new JavaTimeModule());
            builder.featuresToDisable(
                    SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    SerializationFeature.FAIL_ON_EMPTY_BEANS);
        };
    }

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        return mapper;
    }
}
