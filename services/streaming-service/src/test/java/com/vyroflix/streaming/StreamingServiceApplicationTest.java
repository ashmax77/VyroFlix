package com.vyroflix.streaming;

import com.vyroflix.streaming.service.StreamingService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
class StreamingServiceApplicationTest {

    @MockBean
    private StreamingService streamingService;

    @MockBean
    private com.vyroflix.streaming.consumer.VideoEncodedConsumer videoEncodedConsumer;

    @Test
    void contextLoads() {
    }
}
