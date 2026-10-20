package com.vyroflix.encoding;

import com.vyroflix.encoding.consumer.VideoUploadedConsumer;
import com.vyroflix.encoding.service.EncodingService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
class EncodingServiceApplicationTest {

    @MockBean
    private EncodingService encodingService;

    @MockBean
    private VideoUploadedConsumer videoUploadedConsumer;

    @Test
    void contextLoads() {
    }
}
