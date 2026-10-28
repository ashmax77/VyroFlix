package com.vyroflix.platform;

import com.vyroflix.content.service.AdminCatalogService;
import com.vyroflix.content.service.CatalogService;
import com.vyroflix.history.service.HistoryService;
import com.vyroflix.identity.seeder.AdminUserSeeder;
import com.vyroflix.identity.service.UserService;
import com.vyroflix.recommendation.service.RecommendationService;
import com.vyroflix.search.service.SearchService;
import com.vyroflix.streaming.service.StreamingService;
import com.vyroflix.video.service.UploadService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
class PlatformApplicationTest {

    @MockBean
    private UserService userService;

    @MockBean
    private AdminUserSeeder adminUserSeeder;

    @MockBean
    private AdminCatalogService adminCatalogService;

    @MockBean
    private CatalogService catalogService;

    @MockBean
    private com.vyroflix.content.service.CastService castService;

    @MockBean
    private com.vyroflix.content.service.CollectionService collectionService;

    @MockBean
    private com.vyroflix.encoding.repository.EncodingJobRepository encodingJobRepository;

    @MockBean
    private com.vyroflix.recommendation.service.RecommendationEventProcessor recommendationEventProcessor;

    @MockBean
    private StreamingService streamingService;

    @MockBean
    private HistoryService historyService;

    @MockBean
    private UploadService uploadService;

    @MockBean
    private SearchService searchService;

    @MockBean
    private RecommendationService recommendationService;

    @MockBean
    private com.vyroflix.content.consumer.VideoEncodedConsumer contentVideoEncodedConsumer;

    @MockBean
    private com.vyroflix.streaming.consumer.VideoEncodedConsumer streamingVideoEncodedConsumer;

    @MockBean
    private com.vyroflix.search.consumer.CatalogPublishedConsumer searchCatalogPublishedConsumer;

    @MockBean
    private com.vyroflix.search.consumer.CatalogUnpublishedConsumer searchCatalogUnpublishedConsumer;

    @MockBean
    private com.vyroflix.recommendation.consumer.CatalogPublishedConsumer recommendationCatalogPublishedConsumer;

    @MockBean
    private com.vyroflix.recommendation.consumer.PlaybackStartedConsumer recommendationPlaybackStartedConsumer;

    @MockBean
    private com.vyroflix.recommendation.consumer.ContentCompletedConsumer recommendationContentCompletedConsumer;

    @Test
    @DisplayName("Combined platform application context loads successfully with all aggregated domain services")
    void contextLoads() {
    }
}
