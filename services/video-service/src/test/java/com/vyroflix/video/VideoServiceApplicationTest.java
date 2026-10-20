package com.vyroflix.video;

import com.vyroflix.video.service.UploadService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
class VideoServiceApplicationTest {

    @MockBean
    private UploadService uploadService;

    @Test
    void contextLoads() {
    }
}
