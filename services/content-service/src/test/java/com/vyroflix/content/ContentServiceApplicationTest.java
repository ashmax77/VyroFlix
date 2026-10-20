package com.vyroflix.content;

import com.vyroflix.content.service.AdminCatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
class ContentServiceApplicationTest {

    @MockBean
    private AdminCatalogService adminCatalogService;

    @MockBean
    private com.vyroflix.content.service.CatalogService catalogService;

    @MockBean
    private com.vyroflix.content.consumer.VideoEncodedConsumer videoEncodedConsumer;

    @Test
    void contextLoads() {
    }
}
