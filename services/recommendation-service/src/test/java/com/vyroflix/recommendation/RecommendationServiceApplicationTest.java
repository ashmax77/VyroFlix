package com.vyroflix.recommendation;

import com.vyroflix.recommendation.consumer.CatalogPublishedConsumer;
import com.vyroflix.recommendation.consumer.ContentCompletedConsumer;
import com.vyroflix.recommendation.consumer.PlaybackStartedConsumer;
import com.vyroflix.recommendation.service.RecommendationEventProcessor;
import com.vyroflix.recommendation.service.RecommendationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
class RecommendationServiceApplicationTest {

    @MockBean
    private RecommendationService recommendationService;

    @MockBean
    private RecommendationEventProcessor recommendationEventProcessor;

    @MockBean
    private CatalogPublishedConsumer catalogPublishedConsumer;

    @MockBean
    private PlaybackStartedConsumer playbackStartedConsumer;

    @MockBean
    private ContentCompletedConsumer contentCompletedConsumer;

    @Test
    void contextLoads() {
    }
}
