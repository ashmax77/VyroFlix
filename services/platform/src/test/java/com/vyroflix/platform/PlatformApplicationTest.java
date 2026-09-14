package com.vyroflix.platform;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
class PlatformApplicationTest {

    @Test
    @DisplayName("Combined platform application context loads successfully with all aggregated domain services")
    void contextLoads() {
    }
}
