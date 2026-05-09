package com.example.securityanalyze.collector.infrastructure;

import com.example.securityanalyze.common.RepositoryTestBase;
import com.example.securityanalyze.common.TestDataFactory;
import com.example.securityanalyze.collector.domain.CollectorOverview;
import com.example.securityanalyze.collector.domain.CollectorTask;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Import(CollectorDashboardRepositoryImpl.class)
class CollectorDashboardRepositoryTest extends RepositoryTestBase {

    @Autowired
    private CollectorDashboardRepositoryImpl collectorDashboardRepository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    void shouldFindOverview() {
        // 准备 company / security / financial_report 数据
        Long companyId = TestDataFactory.insertCompany(jdbcTemplate, TestDataFactory.company("91110027", "概览公司", "概览"));
        TestDataFactory.insertCompanySecurity(jdbcTemplate, TestDataFactory.security(companyId, "600011", "概览股"));
        TestDataFactory.insertFinancialReport(jdbcTemplate, TestDataFactory.report("600011", java.time.LocalDate.of(2023, 12, 31)));

        // 准备最近任务日志
        LocalDateTime now = LocalDateTime.now();
        TestDataFactory.insertCollectorTaskLog(jdbcTemplate, "公司采集", "company", now.minusHours(2), now.minusHours(1), "success", 100);
        TestDataFactory.insertCollectorTaskLog(jdbcTemplate, "证券采集", "security", now.minusHours(3), now.minusHours(2), "success", 50);
        TestDataFactory.insertCollectorTaskLog(jdbcTemplate, "财报采集", "finance_report", now.minusHours(4), null, "running", 0);

        List<CollectorOverview> results = collectorDashboardRepository.findOverview();

        assertEquals(3, results.size());

        CollectorOverview companyItem = results.stream().filter(r -> "company".equals(r.getDataType())).findFirst().orElseThrow();
        assertEquals("公司基本信息", companyItem.getDataTypeLabel());
        assertTrue(companyItem.getTotalRows() >= 1);
        assertEquals("success", companyItem.getLastTaskStatus());
        assertNotNull(companyItem.getLastTaskDurationSeconds());

        CollectorOverview financeItem = results.stream().filter(r -> "finance_report".equals(r.getDataType())).findFirst().orElseThrow();
        assertEquals("running", financeItem.getLastTaskStatus());
    }

    @Test
    void shouldFindTasks() {
        LocalDateTime now = LocalDateTime.now();
        TestDataFactory.insertCollectorTaskLog(jdbcTemplate, "任务A", "company", now.minusHours(1), now, "success", 100);
        TestDataFactory.insertCollectorTaskLog(jdbcTemplate, "任务B", "company", now.minusHours(2), now.minusHours(1), "failed", 0);
        TestDataFactory.insertCollectorTaskLog(jdbcTemplate, "任务C", "security", now.minusHours(3), now.minusHours(2), "success", 50);

        // 无条件查询（默认7天内）
        List<CollectorTask> all = collectorDashboardRepository.findTasks(null, null, 0, 10);
        assertTrue(all.size() >= 3);

        // 按 dataType 过滤
        List<CollectorTask> companyTasks = collectorDashboardRepository.findTasks("company", null, 0, 10);
        assertEquals(2, companyTasks.size());

        // 按 status 过滤
        List<CollectorTask> failedTasks = collectorDashboardRepository.findTasks(null, "failed", 0, 10);
        assertEquals(1, failedTasks.size());
        assertEquals("任务B", failedTasks.get(0).getTaskName());
    }

    @Test
    void shouldCountTasks() {
        LocalDateTime now = LocalDateTime.now();
        TestDataFactory.insertCollectorTaskLog(jdbcTemplate, "任务D", "company", now.minusHours(1), now, "success", 10);
        TestDataFactory.insertCollectorTaskLog(jdbcTemplate, "任务E", "company", now.minusHours(2), now.minusHours(1), "success", 20);
        TestDataFactory.insertCollectorTaskLog(jdbcTemplate, "任务F", "security", now.minusHours(3), now.minusHours(2), "failed", 0);

        long allCount = collectorDashboardRepository.countTasks(null, null);
        long companyCount = collectorDashboardRepository.countTasks("company", null);
        long failedCount = collectorDashboardRepository.countTasks(null, "failed");

        assertTrue(allCount >= 3);
        assertEquals(2L, companyCount);
        assertEquals(1L, failedCount);
    }

    @Test
    void shouldHandleEmptyTablesInOverview() {
        // 未插入任何数据，验证空表时各字段为 null 或 0
        List<CollectorOverview> results = collectorDashboardRepository.findOverview();

        assertEquals(3, results.size());
        for (CollectorOverview item : results) {
            assertEquals(0, item.getTotalRows(), "空表时 total_rows 应为 0");
            assertNull(item.getLastUpdatedAt(), "空表时 last_updated_at 应为 null");
            assertNull(item.getLastTaskStatus(), "无任务时 last_task_status 应为 null");
            assertNull(item.getLastTaskDurationSeconds(), "无任务时 duration 应为 null");
        }
    }

    @Test
    void shouldReturnNullDurationForRunningTask() {
        LocalDateTime now = LocalDateTime.now();
        TestDataFactory.insertCollectorTaskLog(jdbcTemplate, "运行中任务", "company", now.minusMinutes(30), null, "running", 0);

        List<CollectorTask> tasks = collectorDashboardRepository.findTasks(null, null, 0, 10);

        CollectorTask runningTask = tasks.stream()
                .filter(t -> "运行中任务".equals(t.getTaskName()))
                .findFirst()
                .orElseThrow();
        assertEquals("running", runningTask.getStatus());
        assertNull(runningTask.getDurationSeconds(), "ended_at 为 null 时 duration_seconds 应为 null");
    }

    @Test
    void shouldFindTasksWithCombinedFilters() {
        LocalDateTime now = LocalDateTime.now();
        TestDataFactory.insertCollectorTaskLog(jdbcTemplate, "组合A", "company", now.minusHours(1), now, "success", 10);
        TestDataFactory.insertCollectorTaskLog(jdbcTemplate, "组合B", "company", now.minusHours(2), now.minusHours(1), "failed", 0);
        TestDataFactory.insertCollectorTaskLog(jdbcTemplate, "组合C", "security", now.minusHours(3), now.minusHours(2), "success", 5);

        List<CollectorTask> combined = collectorDashboardRepository.findTasks("company", "success", 0, 10);

        assertEquals(1, combined.size());
        assertEquals("组合A", combined.get(0).getTaskName());
    }

    @Test
    void shouldReturnEmptyWhenOffsetExceeds() {
        LocalDateTime now = LocalDateTime.now();
        TestDataFactory.insertCollectorTaskLog(jdbcTemplate, "分页", "company", now.minusHours(1), now, "success", 1);

        List<CollectorTask> results = collectorDashboardRepository.findTasks(null, null, 1000, 10);
        assertTrue(results.isEmpty());
    }
}
