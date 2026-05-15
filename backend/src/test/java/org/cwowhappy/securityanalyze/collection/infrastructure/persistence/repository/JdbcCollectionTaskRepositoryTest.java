package org.cwowhappy.securityanalyze.collection.infrastructure.persistence.repository;

import org.cwowhappy.securityanalyze.collection.domain.model.CollectionTask;
import org.cwowhappy.securityanalyze.collection.domain.model.CollectionTaskId;
import org.cwowhappy.securityanalyze.collection.domain.repository.CollectionTaskRepository;
import org.cwowhappy.securityanalyze.shared.dto.PageQuery;
import org.cwowhappy.securityanalyze.shared.dto.PageResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JdbcCollectionTaskRepository 集成测试。
 * 使用 Testcontainers 启动真实 PostgreSQL，验证采集任务的持久化与状态查询能力。
 */
@Tag("integration")
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JdbcCollectionTaskRepositoryTest {

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
    private CollectionTaskRepository collectionTaskRepository;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Test
    @Transactional
    void shouldSaveAndFindTaskById() {
        // 给定
        CollectionTask task = buildTask("stock_full", "pending", "akshare");

        // 当
        CollectionTaskId savedId = collectionTaskRepository.save(task);
        Optional<CollectionTask> found = collectionTaskRepository.findById(savedId);

        // 则
        assertThat(found).isPresent();
        CollectionTask result = found.get();
        assertThat(result.getId()).isEqualTo(savedId);
        assertThat(result.getTaskType()).isEqualTo("stock_full");
        assertThat(result.getStatus()).isEqualTo("pending");
        assertThat(result.getDataSource()).isEqualTo("akshare");
        assertThat(result.getTotalCount()).isZero();
        assertThat(result.getSuccessCount()).isZero();
        assertThat(result.getFailCount()).isZero();
    }

    @Test
    @Transactional
    void shouldFindTasksByPageAndStatus() {
        // 给定：2 条 pending，1 条 running
        collectionTaskRepository.save(buildTask("stock_full", "pending", "akshare"));
        collectionTaskRepository.save(buildTask("stock_daily", "pending", "tushare"));
        collectionTaskRepository.save(buildTask("stock_full", "running", "akshare"));

        // 当：按 pending 状态分页查询
        PageQuery query = new PageQuery();
        query.setPage(1);
        query.setSize(10);
        PageResult<CollectionTask> page = collectionTaskRepository.findByPage(query, "pending", null);

        // 则
        assertThat(page.getTotal()).isEqualTo(2L);
        assertThat(page.getList()).hasSize(2);
        assertThat(page.getList())
                .extracting(CollectionTask::getStatus)
                .containsOnly("pending");
    }

    @Test
    @Transactional
    void shouldUpdateTaskStatus() {
        // 给定：先保存一条 pending 状态的任务
        CollectionTaskId id = CollectionTaskId.generate();
        CollectionTask task = CollectionTask.builder()
                .id(id)
                .taskType("stock_full")
                .status("pending")
                .dataSource("akshare")
                .totalCount(0)
                .successCount(0)
                .failCount(0)
                .build();
        collectionTaskRepository.save(task);

        // 当：更新状态为 running 后再次保存（Upsert）
        CollectionTask updated = CollectionTask.builder()
                .id(id)
                .taskType("stock_full")
                .status("running")
                .dataSource("akshare")
                .totalCount(100)
                .successCount(50)
                .failCount(0)
                .build();
        collectionTaskRepository.save(updated);

        // 则
        Optional<CollectionTask> found = collectionTaskRepository.findById(id);
        assertThat(found).isPresent();
        CollectionTask result = found.get();
        assertThat(result.getStatus()).isEqualTo("running");
        assertThat(result.getTotalCount()).isEqualTo(100);
        assertThat(result.getSuccessCount()).isEqualTo(50);
    }

    @Test
    @Transactional
    void shouldReturnMonitorOverview() {
        // 先插入父任务以满足外键约束
        namedParameterJdbcTemplate.update(
                "INSERT INTO tb_collection_task (id, task_type, status, data_source, total_count, success_count, fail_count) VALUES ('task-1', 'stock_full', 'success', 'akshare', 0, 0, 0)",
                new MapSqlParameterSource());

        // 插入测试数据到 tb_collection_stock_state
        String insertSql = """
                INSERT INTO tb_collection_stock_state (id, task_id, stock_code, task_type, status, error_message, updated_at)
                VALUES
                    (:id1, 'task-1', '000001', 'stock_basic', 'success', null, NOW()),
                    (:id2, 'task-1', '000002', 'stock_basic', 'success', null, NOW() - INTERVAL '25 hours'),
                    (:id3, 'task-1', '000003', 'stock_basic', 'failed', null, NOW()),
                    (:id4, 'task-1', '000001', 'company_info', 'success', null, NOW())
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id1", UUID.randomUUID());
        params.addValue("id2", UUID.randomUUID());
        params.addValue("id3", UUID.randomUUID());
        params.addValue("id4", UUID.randomUUID());
        namedParameterJdbcTemplate.update(insertSql, params);

        var result = collectionTaskRepository.findMonitorOverview(24);

        org.assertj.core.api.Assertions.assertThat(result).hasSize(2);
        var stockBasic = result.stream().filter(r -> r.getTaskType().equals("stock_basic")).findFirst().orElseThrow();
        org.assertj.core.api.Assertions.assertThat(stockBasic.getTotalCount()).isEqualTo(3);
        org.assertj.core.api.Assertions.assertThat(stockBasic.getRecentSuccessCount()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(stockBasic.getRecentExpiredCount()).isEqualTo(1);
    }

    @Test
    @Transactional
    void shouldReturnBaselineCount() {
        Long count = collectionTaskRepository.countAllStocks();
        org.assertj.core.api.Assertions.assertThat(count).isGreaterThanOrEqualTo(0);
    }

    private CollectionTask buildTask(String taskType, String status, String dataSource) {
        return CollectionTask.builder()
                .id(CollectionTaskId.generate())
                .taskType(taskType)
                .mode("full")
                .status(status)
                .dataSource(dataSource)
                .totalCount(0)
                .successCount(0)
                .failCount(0)
                .build();
    }
}
