package com.example.securityanalyze.index.infrastructure;

import com.example.securityanalyze.common.RepositoryTestBase;
import com.example.securityanalyze.common.TestDataFactory;
import com.example.securityanalyze.index.domain.IndexEtfMapping;
import com.example.securityanalyze.index.domain.IndexEtfMappingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Import(IndexEtfMappingRepositoryImpl.class)
class IndexEtfMappingRepositoryImplTest extends RepositoryTestBase {

    @Autowired
    private IndexEtfMappingRepository indexEtfMappingRepository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    void shouldFindByIndexCode() {
        TestDataFactory.insertIndexEtfMapping(jdbcTemplate,
                TestDataFactory.indexEtfMapping("000300", "510300"));
        TestDataFactory.insertIndexEtfMapping(jdbcTemplate,
                TestDataFactory.indexEtfMapping("000300", "510330"));

        List<IndexEtfMapping> results = indexEtfMappingRepository.findByIndexCode("000300");

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(m -> "000300".equals(m.getIndexCode())));
    }

    @Test
    void shouldFindByEtfCode() {
        TestDataFactory.insertIndexEtfMapping(jdbcTemplate,
                TestDataFactory.indexEtfMapping("000300", "510300"));
        TestDataFactory.insertIndexEtfMapping(jdbcTemplate,
                TestDataFactory.indexEtfMapping("000016", "510050"));

        List<IndexEtfMapping> results = indexEtfMappingRepository.findByEtfCode("510300");

        assertEquals(1, results.size());
        assertEquals("000300", results.get(0).getIndexCode());
    }

    @Test
    void shouldReturnEmptyWhenIndexCodeNotFound() {
        List<IndexEtfMapping> results = indexEtfMappingRepository.findByIndexCode("999999");
        assertTrue(results.isEmpty());
    }
}
