package org.cwowhappy.securityanalyze.user.infrastructure.persistence.repository;

import org.cwowhappy.securityanalyze.user.domain.repository.TokenSessionRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JdbcTokenSessionRepository 集成测试。
 */
@Tag("integration")
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JdbcTokenSessionRepositoryTest {

    private static final String POSTGRES_IMAGE = "postgres:" + System.getenv().getOrDefault("TESTCONTAINERS_POSTGRES_VERSION", "16");
    private static final String TEST_DB_NAME = System.getenv().getOrDefault("TEST_DB_NAME", "db-security-analyze");
    private static final String TEST_DB_USER = System.getenv().getOrDefault("TEST_DB_USER", "test");
    private static final String TEST_DB_PASSWORD = System.getenv().getOrDefault("TEST_DB_PASSWORD", "test");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE)
            .withDatabaseName(TEST_DB_NAME)
            .withUsername(TEST_DB_USER)
            .withPassword(TEST_DB_PASSWORD);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TokenSessionRepository tokenSessionRepository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    private void insertUser(String userId, String username, String email) {
        String sql = """
                INSERT INTO tb_user (id, username, email, password_hash, role, created_at, updated_at)
                VALUES (:id, :username, :email, :password, :role, NOW(), NOW())
                ON CONFLICT (id) DO NOTHING
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", userId);
        params.addValue("username", username);
        params.addValue("email", email);
        params.addValue("password", "pass");
        params.addValue("role", "viewer");
        jdbcTemplate.update(sql, params);
    }

    @Test
    @Transactional
    void shouldSaveAndFindToken() {
        insertUser("user001", "user001", "u1@example.com");

        String tokenHash = "hash-abc-123";
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

        tokenSessionRepository.save("user001", tokenHash, expiresAt);

        assertThat(tokenSessionRepository.existsByTokenHash(tokenHash)).isTrue();
    }

    @Test
    @Transactional
    void shouldReturnFalseWhenTokenNotExists() {
        assertThat(tokenSessionRepository.existsByTokenHash("nonexistent")).isFalse();
    }

    @Test
    @Transactional
    void shouldDeleteToken() {
        insertUser("user001", "user001", "u1@example.com");

        String tokenHash = "hash-to-delete";
        tokenSessionRepository.save("user001", tokenHash, LocalDateTime.now().plusHours(1));

        int deleted = tokenSessionRepository.deleteByTokenHash(tokenHash);

        assertThat(deleted).isEqualTo(1);
        assertThat(tokenSessionRepository.existsByTokenHash(tokenHash)).isFalse();
    }

    @Test
    @Transactional
    void shouldDeleteExpiredSessions() {
        insertUser("user001", "user001", "u1@example.com");

        String oldHash = "hash-expired";
        tokenSessionRepository.save("user001", oldHash, LocalDateTime.now().minusMinutes(1));

        int deleted = tokenSessionRepository.deleteExpiredSessions();

        assertThat(deleted).isGreaterThanOrEqualTo(0);
    }
}
