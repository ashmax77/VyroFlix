package com.vyroflix.history;

import com.vyroflix.history.service.HistoryService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
class HistoryServiceApplicationTest {

    @MockBean
    private HistoryService historyService;

    @Test
    void contextLoads() {
    }
}
