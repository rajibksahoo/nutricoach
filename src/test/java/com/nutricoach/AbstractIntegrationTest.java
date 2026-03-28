package com.nutricoach;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class for all integration tests.
 *
 * Default: connects to the local pg-test container via application-test.yml
 *   (url: jdbc:postgresql://localhost:5433/nutricoach_test)
 *   Start it once with:
 *     docker run -d --name pg-test -p 5433:5432 \
 *       -e POSTGRES_DB=nutricoach_test -e POSTGRES_USER=nutricoach \
 *       -e POSTGRES_PASSWORD=nutricoach postgres:16-alpine
 *   Then just run tests from IntelliJ — no env vars needed.
 *
 * CI override: set TEST_DB_URL to point at a CI-managed database.
 *   All other test config (external service stubs, feature flags) lives in
 *   application-test.yml and does not require env vars.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    private static final String TEST_DB_URL = System.getenv("TEST_DB_URL");

    /**
     * CI hook: when TEST_DB_URL is set, override the datasource URL from application-test.yml.
     * On a developer machine this method does nothing — the yml config is used as-is.
     */
    @DynamicPropertySource
    static void overrideDataSourceForCi(DynamicPropertyRegistry registry) {
        if (TEST_DB_URL != null && !TEST_DB_URL.isBlank()) {
            registry.add("spring.datasource.url", () -> TEST_DB_URL);
            registry.add("spring.datasource.username", () -> "nutricoach");
            registry.add("spring.datasource.password", () -> "nutricoach");
            registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        }
    }
}
