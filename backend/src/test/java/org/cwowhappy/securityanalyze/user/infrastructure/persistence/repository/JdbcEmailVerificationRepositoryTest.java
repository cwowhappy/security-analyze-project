package org.cwowhappy.securityanalyze.user.infrastructure.persistence.repository;

import org.cwowhappy.securityanalyze.user.domain.model.EmailVerification;
import org.cwowhappy.securityanalyze.user.domain.model.UserId;
import org.cwowhappy.securityanalyze.user.domain.repository.EmailVerificationRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JdbcEmailVerificationRepository 集成测试。
 */
@Tag("integration")
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JdbcEmailVerificationRepositoryTest {

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
    private EmailVerificationRepository emailVerificationRepository;

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
    void shouldSaveAndFindLatestByUserId() {
        insertUser("user001", "user001", "u1@example.com");

        EmailVerification verification = EmailVerification.builder()
                .userId("user001")
                .verificationCode("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();

        emailVerificationRepository.save(verification);

        Optional<EmailVerification> found = emailVerificationRepository.findLatestByUserId(UserId.of("user001"));
        assertThat(found).isPresent();
        assertThat(found.get().getVerificationCode()).isEqualTo("123456");
    }

    @Test
    @Transactional
    void shouldNotFindUsedVerification() {
        insertUser("user002", "user002", "u2@example.com");

        EmailVerification verification = EmailVerification.builder()
                .userId("user002")
                .verificationCode("654321")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .used(true)
                .createdAt(LocalDateTime.now())
                .build();

        emailVerificationRepository.save(verification);

        Optional<EmailVerification> found = emailVerificationRepository.findLatestByUserId(UserId.of("user002"));
        assertThat(found).isEmpty();
    }

    @Test
    @Transactional
    void shouldMarkAsUsed() {
        insertUser("user003", "user003", "u3@example.com");

        EmailVerification verification = EmailVerification.builder()
                .userId("user003")
                .verificationCode("111111")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();

        emailVerificationRepository.save(verification);
        Optional<EmailVerification> before = emailVerificationRepository.findLatestByUserId(UserId.of("user003"));
        assertThat(before).isPresent();

        emailVerificationRepository.markAsUsed(before.get().getId());

        Optional<EmailVerification> after = emailVerificationRepository.findLatestByUserId(UserId.of("user003"));
        assertThat(after).isEmpty();
    }
}
