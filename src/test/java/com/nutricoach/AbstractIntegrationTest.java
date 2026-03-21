package com.nutricoach;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for all integration tests.
 *
 * Two modes:
 *  1. Testcontainers (default) — auto-starts postgres:16-alpine via Docker.
 *  2. Manual DB — set env var TEST_DB_URL to skip Testcontainers entirely.
 *     Start Postgres manually:
 *       docker run -d --name pg-test -p 5433:5432
 *         -e POSTGRES_DB=nutricoach_test -e POSTGRES_USER=nutricoach
 *         -e POSTGRES_PASSWORD=nutricoach postgres:16-alpine
 *     Then run:
 *       TEST_DB_URL=jdbc:postgresql://localhost:5433/nutricoach_test mvn test
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    private static final String TEST_DB_URL = System.getenv("TEST_DB_URL");
    private static final boolean USE_MANUAL_DB = TEST_DB_URL != null && !TEST_DB_URL.isBlank();

    static final PostgreSQLContainer<?> POSTGRES;

    static {
        if (USE_MANUAL_DB) {
            POSTGRES = null;
        } else {
            POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("nutricoach_test")
                    .withUsername("nutricoach")
                    .withPassword("nutricoach")
                    .withReuse(true);
            POSTGRES.start();
        }
    }

    @DynamicPropertySource
    static void overrideDataSource(DynamicPropertyRegistry registry) {
        if (USE_MANUAL_DB) {
            registry.add("spring.datasource.url", () -> TEST_DB_URL);
            registry.add("spring.datasource.username", () -> "nutricoach");
            registry.add("spring.datasource.password", () -> "nutricoach");
            registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        } else {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
            registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        }
    }
}