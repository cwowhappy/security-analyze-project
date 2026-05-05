package com.example.securityanalyze.index.infrastructure;

import com.example.securityanalyze.common.RepositoryTestBase;
import com.example.securityanalyze.common.TestDataFactory;
import com.example.securityanalyze.index.domain.IndexHistory;
import com.example.securityanalyze.index.domain.IndexHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Import(IndexHistoryRepositoryImpl.class)
class IndexHistoryRepositoryImplTest extends RepositoryTestBase {

    @Autowired
    private IndexHistoryRepository indexHistoryRepository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    void shouldFindByIndexCodeAndGranularity() {
        TestDataFactory.insertIndexHistory(jdbcTemplate,
                TestDataFactory.indexHistory("000001", LocalDate.of(2024, 1, 1), "day"));
        TestDataFactory.insertIndexHistory(jdbcTemplate,
                TestDataFactory.indexHistory("000001", LocalDate.of(2024, 1, 2), "day"));
        TestDataFactory.insertIndexHistory(jdbcTemplate,
                TestDataFactory.indexHistory("000001", LocalDate.of(2024, 1, 1), "week"));

        List<IndexHistory> dayResults = indexHistoryRepository.findByIndexCodeAndGranularity(
                "000001", "day", null, null);

        assertEquals(2, dayResults.size());
        assertEquals(LocalDate.of(2024, 1, 1), dayResults.get(0).getTradeDate());
    }

    @Test
    void shouldFindWithDateRange() {
        TestDataFactory.insertIndexHistory(jdbcTemplate,
                TestDataFactory.indexHistory("000001", LocalDate.of(2024, 1, 1), "day"));
        TestDataFactory.insertIndexHistory(jdbcTemplate,
                TestDataFactory.indexHistory("000001", LocalDate.of(2024, 1, 15), "day"));
        TestDataFactory.insertIndexHistory(jdbcTemplate,
                TestDataFactory.indexHistory("000001", LocalDate.of(2024, 2, 1), "day"));

        List<IndexHistory> results = indexHistoryRepository.findByIndexCodeAndGranularity(
                "000001", "day", LocalDate.of(2024, 1, 10), LocalDate.of(2024, 1, 20));

        assertEquals(1, results.size());
        assertEquals(LocalDate.of(2024, 1, 15), results.get(0).getTradeDate());
    }

    @Test
    void shouldFindByIndexCodeAndGranularityWithPagination() {
        for (int i = 0; i < 5; i++) {
            TestDataFactory.insertIndexHistory(jdbcTemplate,
                    TestDataFactory.indexHistory("000001", LocalDate.of(2024, 1, i + 1), "day"));
        }

        List<IndexHistory> results = indexHistoryRepository.findByIndexCodeAndGranularity(
                "000001", "day", 0, 2);

        assertEquals(2, results.size());
    }

    @Test
    void shouldCountByIndexCodeAndGranularity() {
        TestDataFactory.insertIndexHistory(jdbcTemplate,
                TestDataFactory.indexHistory("000001", LocalDate.of(2024, 1, 1), "day"));
        TestDataFactory.insertIndexHistory(jdbcTemplate,
                TestDataFactory.indexHistory("000001", LocalDate.of(2024, 1, 2), "day"));

        long count = indexHistoryRepository.countByIndexCodeAndGranularity("000001", "day");
        assertEquals(2L, count);

        long weekCount = indexHistoryRepository.countByIndexCodeAndGranularity("000001", "week");
        assertEquals(0L, weekCount);
    }

    @Test
    void shouldReturnEmptyWhenNoHistory() {
        List<IndexHistory> results = indexHistoryRepository.findByIndexCodeAndGranularity(
                "999999", "day", null, null);
        assertTrue(results.isEmpty());
    }
}
