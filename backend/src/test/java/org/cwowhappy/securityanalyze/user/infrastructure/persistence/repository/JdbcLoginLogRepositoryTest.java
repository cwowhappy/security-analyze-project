package org.cwowhappy.securityanalyze.user.infrastructure.persistence.repository;

import org.cwowhappy.securityanalyze.user.domain.model.LoginLog;
import org.cwowhappy.securityanalyze.user.domain.model.User;
import org.cwowhappy.securityanalyze.user.domain.model.UserId;
import org.cwowhappy.securityanalyze.user.domain.repository.LoginLogRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JdbcLoginLogRepository 集成测试。
 */
@Tag("integration")
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JdbcLoginLogRepositoryTest {

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
    private LoginLogRepository loginLogRepository;

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
    void shouldSaveAndFindByConditions() {
        LoginLog log = LoginLog.builder()
                .userId("user001")
                .username("testuser")
                .action("login_success")
                .ip("127.0.0.1")
                .userAgent("Mozilla")
                .details("登录成功")
                .createdAt(LocalDateTime.now())
                .build();
        loginLogRepository.save(log);

        List<LoginLog> results = loginLogRepository.findByConditions("user001", null, null, null, 1, 20);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAction()).isEqualTo("login_success");
    }

    @Test
    @Transactional
    void shouldCountByConditions() {
        LoginLog log = LoginLog.builder()
                .userId("user001")
                .username("testuser")
                .action("login_failed")
                .ip("127.0.0.1")
                .userAgent("Mozilla")
                .details("密码错误")
                .createdAt(LocalDateTime.now())
                .build();
        loginLogRepository.save(log);

        long count = loginLogRepository.countByConditions(null, "login_failed", null, null);
        assertThat(count).isEqualTo(1);
    }

    @Test
    @Transactional
    void shouldFilterByAction() {
        LoginLog log1 = LoginLog.builder()
                .userId("user001")
                .username("testuser")
                .action("login_success")
                .ip("127.0.0.1")
                .userAgent("Mozilla")
                .createdAt(LocalDateTime.now())
                .build();
        LoginLog log2 = LoginLog.builder()
                .userId("user001")
                .username("testuser")
                .action("logout")
                .ip("127.0.0.1")
                .userAgent("Mozilla")
                .createdAt(LocalDateTime.now())
                .build();
        loginLogRepository.save(log1);
        loginLogRepository.save(log2);

        List<LoginLog> results = loginLogRepository.findByConditions(null, "logout", null, null, 1, 20);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAction()).isEqualTo("logout");
    }
}
