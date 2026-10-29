package com.vyroflix.identity;

import com.vyroflix.identity.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
class IdentityServiceApplicationTest {

    @MockBean
    private UserService userService;


    @Test
    void contextLoads() {
    }
}
