package io.github.springxpose.sample.rest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test — verifies the Spring application context loads cleanly with
 * all generated controllers, repositories, and security configurers present.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SampleRestApplicationTest {

    @Test
    void contextLoads() {
        // If this test passes the full context (including all generated beans) started OK.
    }
}
