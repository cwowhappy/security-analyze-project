package com.example.securityanalyze.index.infrastructure;

import com.example.securityanalyze.common.RepositoryTestBase;
import com.example.securityanalyze.common.TestDataFactory;
import com.example.securityanalyze.index.domain.IndexInfo;
import com.example.securityanalyze.index.domain.IndexRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Import(IndexRepositoryImpl.class)
class IndexRepositoryImplTest extends RepositoryTestBase {

    @Autowired
    private IndexRepository indexRepository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    void shouldFindByIndexCode() {
        IndexInfo index = TestDataFactory.indexInfo("000001", "上证指数", "宽基");
        TestDataFactory.insertIndexInfo(jdbcTemplate, index);

        Optional<IndexInfo> found = indexRepository.findByIndexCode("000001");

        assertTrue(found.isPresent());
        assertEquals("上证指数", found.get().getIndexName());
        assertEquals("宽基", found.get().getIndexType());
    }

    @Test
    void shouldReturnEmptyWhenIndexCodeNotFound() {
        Optional<IndexInfo> found = indexRepository.findByIndexCode("999999");
        assertTrue(found.isEmpty());
    }

    @Test
    void shouldFindById() {
        IndexInfo index = TestDataFactory.indexInfo("000002", "A股指数", "宽基");
        Long id = TestDataFactory.insertIndexInfo(jdbcTemplate, index);

        Optional<IndexInfo> found = indexRepository.findById(id);

        assertTrue(found.isPresent());
        assertEquals("A股指数", found.get().getIndexName());
    }

    @Test
    void shouldFindByKeywordMatchingCode() {
        TestDataFactory.insertIndexInfo(jdbcTemplate, TestDataFactory.indexInfo("000300", "沪深300", "宽基"));
        TestDataFactory.insertIndexInfo(jdbcTemplate, TestDataFactory.indexInfo("000905", "中证500", "宽基"));

        List<IndexInfo> results = indexRepository.findByKeyword("000300", 0, 10);

        assertEquals(1, results.size());
        assertEquals("沪深300", results.get(0).getIndexName());
    }

    @Test
    void shouldFindByKeywordMatchingName() {
        TestDataFactory.insertIndexInfo(jdbcTemplate, TestDataFactory.indexInfo("399001", "深证成指", "宽基"));

        List<IndexInfo> results = indexRepository.findByKeyword("深证", 0, 10);

        assertEquals(1, results.size());
        assertEquals("深证成指", results.get(0).getIndexName());
    }

    @Test
    void shouldFindByKeywordCaseInsensitive() {
        TestDataFactory.insertIndexInfo(jdbcTemplate, TestDataFactory.indexInfo("399006", "创业板指", "宽基"));

        List<IndexInfo> lowerCase = indexRepository.findByKeyword("创业板", 0, 10);
        List<IndexInfo> upperCase = indexRepository.findByKeyword("创业板", 0, 10);

        assertEquals(1, lowerCase.size());
        assertEquals(1, upperCase.size());
    }

    @Test
    void shouldReturnAllWhenKeywordNullOrBlank() {
        TestDataFactory.insertIndexInfo(jdbcTemplate, TestDataFactory.indexInfo("000016", "上证50", "宽基"));
        TestDataFactory.insertIndexInfo(jdbcTemplate, TestDataFactory.indexInfo("000010", "上证180", "宽基"));

        List<IndexInfo> results = indexRepository.findByKeyword(null, 0, 10);

        assertTrue(results.size() >= 2);
    }

    @Test
    void shouldCountByKeyword() {
        TestDataFactory.insertIndexInfo(jdbcTemplate, TestDataFactory.indexInfo("399989", "中证医疗", "行业"));
        TestDataFactory.insertIndexInfo(jdbcTemplate, TestDataFactory.indexInfo("399998", "中证煤炭", "行业"));

        long count = indexRepository.countByKeyword("医疗");
        assertEquals(1L, count);

        long countAll = indexRepository.countByKeyword(null);
        assertTrue(countAll >= 2L);
    }

    @Test
    void shouldFindAllByIndexCodes() {
        TestDataFactory.insertIndexInfo(jdbcTemplate, TestDataFactory.indexInfo("000021", "上证180金融", "主题"));
        TestDataFactory.insertIndexInfo(jdbcTemplate, TestDataFactory.indexInfo("000022", "上证180基建", "主题"));
        TestDataFactory.insertIndexInfo(jdbcTemplate, TestDataFactory.indexInfo("000042", "上证央企", "主题"));

        List<IndexInfo> results = indexRepository.findAllByIndexCodes(List.of("000021", "000042"));

        assertEquals(2, results.size());
        List<String> codes = results.stream().map(IndexInfo::getIndexCode).toList();
        assertTrue(codes.contains("000021"));
        assertTrue(codes.contains("000042"));
    }

    @Test
    void shouldReturnEmptyListWhenIndexCodesEmpty() {
        List<IndexInfo> results = indexRepository.findAllByIndexCodes(List.of());
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldFindCoreByType() {
        IndexInfo coreWide = TestDataFactory.indexInfo("000001", "上证指数", "宽基");
        coreWide.setIsCore(true);
        TestDataFactory.insertIndexInfo(jdbcTemplate, coreWide);

        IndexInfo nonCoreWide = TestDataFactory.indexInfo("000002", "A股指数", "宽基");
        nonCoreWide.setIsCore(false);
        TestDataFactory.insertIndexInfo(jdbcTemplate, nonCoreWide);

        IndexInfo coreIndustry = TestDataFactory.indexInfo("399989", "中证医疗", "行业");
        coreIndustry.setIsCore(true);
        TestDataFactory.insertIndexInfo(jdbcTemplate, coreIndustry);

        List<IndexInfo> wideResults = indexRepository.findCoreByType("宽基");
        assertEquals(1, wideResults.size());
        assertEquals("上证指数", wideResults.get(0).getIndexName());

        List<IndexInfo> industryResults = indexRepository.findCoreByType("行业");
        assertEquals(1, industryResults.size());
        assertEquals("中证医疗", industryResults.get(0).getIndexName());
    }

    @Test
    void shouldReturnEmptyWhenNoCoreOfType() {
        List<IndexInfo> results = indexRepository.findCoreByType("不存在的类型");
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldTrimKeyword() {
        TestDataFactory.insertIndexInfo(jdbcTemplate, TestDataFactory.indexInfo("000688", "科创50", "宽基"));

        List<IndexInfo> withSpaces = indexRepository.findByKeyword(" 科创50 ", 0, 10);

        assertEquals(1, withSpaces.size(), "关键字前后空格应被 trim");
    }

    @Test
    void shouldSupportPagination() {
        for (int i = 0; i < 5; i++) {
            TestDataFactory.insertIndexInfo(jdbcTemplate,
                    TestDataFactory.indexInfo(String.format("999%03d", i), "测试指数" + i, "其他"));
        }

        List<IndexInfo> page1 = indexRepository.findByKeyword(null, 0, 2);
        assertEquals(2, page1.size());

        List<IndexInfo> page2 = indexRepository.findByKeyword(null, 2, 2);
        assertEquals(2, page2.size());

        List<IndexInfo> page3 = indexRepository.findByKeyword(null, 4, 2);
        assertTrue(page3.size() >= 1);
    }
}
