package org.cwowhappy.securityanalyze.user.infrastructure.persistence.repository;

import org.cwowhappy.securityanalyze.user.domain.model.PasswordReset;
import org.cwowhappy.securityanalyze.user.domain.model.User;
import org.cwowhappy.securityanalyze.user.domain.model.UserId;
import org.cwowhappy.securityanalyze.user.domain.repository.PasswordResetRepository;
import org.cwowhappy.securityanalyze.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
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
 * JdbcPasswordResetRepository 集成测试。
 * 使用 Testcontainers 启动真实 PostgreSQL，验证 JDBC 实现的持久化语义。
 */
@Tag("integration")
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JdbcPasswordResetRepositoryTest {

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
    private PasswordResetRepository passwordResetRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .id(UserId.of("user001"))
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hash")
                .displayName("testuser")
                .role("viewer")
                .avatarInitial("T")
                .active(true)
                .emailVerified(true)
                .failedLoginAttempts(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(user);
    }

    @Test
    @Transactional
    void shouldSaveAndFindByToken() {
        // 给定
        PasswordReset reset = PasswordReset.builder()
                .userId("user001")
                .resetToken("token-abc-123")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();

        // 当
        passwordResetRepository.save(reset);
        Optional<PasswordReset> found = passwordResetRepository.findByToken("token-abc-123");

        // 则
        assertThat(found).isPresent();
        PasswordReset result = found.get();
        assertThat(result.getUserId()).isEqualTo("user001");
        assertThat(result.getResetToken()).isEqualTo("token-abc-123");
        assertThat(result.isUsed()).isFalse();
    }

    @Test
    @Transactional
    void shouldReturnEmptyWhenTokenNotFound() {
        // 当
        Optional<PasswordReset> found = passwordResetRepository.findByToken("non-existent");

        // 则
        assertThat(found).isEmpty();
    }

    @Test
    @Transactional
    void shouldMarkAsUsed() {
        // 给定
        PasswordReset reset = PasswordReset.builder()
                .userId("user001")
                .resetToken("token-to-use")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();
        passwordResetRepository.save(reset);

        // 当
        Optional<PasswordReset> before = passwordResetRepository.findByToken("token-to-use");
        passwordResetRepository.markAsUsed(before.get().getId());
        Optional<PasswordReset> after = passwordResetRepository.findByToken("token-to-use");

        // 则
        assertThat(before).isPresent();
        assertThat(before.get().isUsed()).isFalse();
        assertThat(after).isPresent();
        assertThat(after.get().isUsed()).isTrue();
    }
}
