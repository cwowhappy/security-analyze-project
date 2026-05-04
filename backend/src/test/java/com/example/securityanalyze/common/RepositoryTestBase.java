package com.example.securityanalyze.common;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Repository 层集成测试抽象基类。
 *
 * <p>使用 @SpringBootTest(webEnvironment = NONE) 加载应用上下文，配合 Testcontainers 启动真实 PostgreSQL 16 容器。
 * 数据库 schema 由 Flyway 在 test profile 下自动迁移（见 application-test.yml）。
 *
 * <p>当前环境使用 Colima 替代 Docker Desktop，Gradle test 任务已配置：
 * <ul>
 *   <li>DOCKER_HOST 指向 Colima 的 Unix Socket</li>
 *   <li>TESTCONTAINERS_RYUK_DISABLED=true 避免 Ryuk 清理容器与 Colima 的兼容问题</li>
 *   <li>api.version=1.53 确保 docker-java 与新版 Docker Engine 的 API 兼容</li>
 * </ul>
 *
 * <p>子类需通过 @Import 显式导入被测的 Repository 实现类，例如：
 * <pre>@Import(UserRepositoryImpl.class)</pre>
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
public abstract class RepositoryTestBase {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
