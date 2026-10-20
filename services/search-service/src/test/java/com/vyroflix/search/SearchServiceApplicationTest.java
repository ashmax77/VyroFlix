package com.vyroflix.search;

import com.vyroflix.search.consumer.CatalogPublishedConsumer;
import com.vyroflix.search.consumer.CatalogUnpublishedConsumer;
import com.vyroflix.search.service.SearchService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
class SearchServiceApplicationTest {

    @MockBean
    private SearchService searchService;

    @MockBean
    private CatalogPublishedConsumer catalogPublishedConsumer;

    @MockBean
    private CatalogUnpublishedConsumer catalogUnpublishedConsumer;

    @Test
    void contextLoads() {
    }
}
