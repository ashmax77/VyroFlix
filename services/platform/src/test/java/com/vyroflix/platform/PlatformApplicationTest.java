package com.vyroflix.platform;

import com.vyroflix.content.service.AdminCatalogService;
import com.vyroflix.content.service.CatalogService;
import com.vyroflix.identity.seeder.AdminUserSeeder;
import com.vyroflix.identity.service.UserService;
import com.vyroflix.history.service.HistoryService;
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
    private StreamingService streamingService;

    @MockBean
    private HistoryService historyService;

    @MockBean
    private UploadService uploadService;

    @Test
    @DisplayName("Combined platform application context loads successfully with all aggregated domain services")
    void contextLoads() {
    }
}
