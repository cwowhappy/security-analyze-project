package com.example.securityanalyze.index.infrastructure;

import com.example.securityanalyze.common.RepositoryTestBase;
import com.example.securityanalyze.common.TestDataFactory;
import com.example.securityanalyze.index.domain.EtfInfo;
import com.example.securityanalyze.index.domain.EtfInfoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Import(EtfInfoRepositoryImpl.class)
class EtfInfoRepositoryImplTest extends RepositoryTestBase {

    @Autowired
    private EtfInfoRepository etfInfoRepository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    void shouldFindByEtfCode() {
        EtfInfo etf = TestDataFactory.etfInfo("510050", "华夏上证50ETF", "000016");
        TestDataFactory.insertEtfInfo(jdbcTemplate, etf);

        Optional<EtfInfo> found = etfInfoRepository.findByEtfCode("510050");

        assertTrue(found.isPresent());
        assertEquals("华夏上证50ETF", found.get().getEtfName());
    }

    @Test
    void shouldFindByTrackingIndexCode() {
        TestDataFactory.insertEtfInfo(jdbcTemplate,
                TestDataFactory.etfInfo("510050", "华夏上证50ETF", "000016"));
        TestDataFactory.insertEtfInfo(jdbcTemplate,
                TestDataFactory.etfInfo("510100", "易方达上证50ETF", "000016"));
        TestDataFactory.insertEtfInfo(jdbcTemplate,
                TestDataFactory.etfInfo("510300", "华泰柏瑞沪深300ETF", "000300"));

        List<EtfInfo> results = etfInfoRepository.findByTrackingIndexCode("000016");

        assertEquals(2, results.size());
    }

    @Test
    void shouldFindByEtfCodes() {
        TestDataFactory.insertEtfInfo(jdbcTemplate,
                TestDataFactory.etfInfo("510050", "华夏上证50ETF", "000016"));
        TestDataFactory.insertEtfInfo(jdbcTemplate,
                TestDataFactory.etfInfo("510300", "华泰柏瑞沪深300ETF", "000300"));
        TestDataFactory.insertEtfInfo(jdbcTemplate,
                TestDataFactory.etfInfo("510500", "南方中证500ETF", "000905"));

        List<EtfInfo> results = etfInfoRepository.findByEtfCodes(List.of("510050", "510500"));

        assertEquals(2, results.size());
    }

    @Test
    void shouldReturnEmptyWhenEtfCodesEmpty() {
        List<EtfInfo> results = etfInfoRepository.findByEtfCodes(List.of());
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenTrackingIndexCodeNotFound() {
        List<EtfInfo> results = etfInfoRepository.findByTrackingIndexCode("999999");
        assertTrue(results.isEmpty());
    }
}
