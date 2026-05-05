package com.example.securityanalyze.index.application;

import com.example.securityanalyze.index.api.*;
import com.example.securityanalyze.index.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IndexServiceTest {

    @Mock
    private IndexRepository indexRepository;

    @Mock
    private IndexHistoryRepository indexHistoryRepository;

    @Mock
    private EtfInfoRepository etfInfoRepository;

    @Mock
    private IndexEtfMappingRepository indexEtfMappingRepository;

    @InjectMocks
    private IndexService indexService;

    @Test
    void shouldListIndexes() {
        IndexInfo idx1 = createIndexInfo("000001", "上证指数", "宽基");
        IndexInfo idx2 = createIndexInfo("399001", "深证成指", "宽基");

        when(indexRepository.findByKeyword(null, 0, 20)).thenReturn(List.of(idx1, idx2));
        when(indexRepository.countByKeyword(null)).thenReturn(2L);

        IndexListResponse response = indexService.listIndexes(null, 0, 20);

        assertEquals(2, response.getItems().size());
        assertEquals(2L, response.getTotal());
        assertEquals("000001", response.getItems().get(0).getIndexCode());
        verify(indexRepository).findByKeyword(null, 0, 20);
        verify(indexRepository).countByKeyword(null);
    }

    @Test
    void shouldListIndexesWithKeyword() {
        IndexInfo idx = createIndexInfo("000300", "沪深300", "宽基");

        when(indexRepository.findByKeyword("沪深", 0, 20)).thenReturn(List.of(idx));
        when(indexRepository.countByKeyword("沪深")).thenReturn(1L);

        IndexListResponse response = indexService.listIndexes("沪深", 0, 20);

        assertEquals(1, response.getItems().size());
        assertEquals("000300", response.getItems().get(0).getIndexCode());
    }

    @Test
    void shouldReturnEmptyList() {
        when(indexRepository.findByKeyword("notexist", 0, 20)).thenReturn(List.of());
        when(indexRepository.countByKeyword("notexist")).thenReturn(0L);

        IndexListResponse response = indexService.listIndexes("notexist", 0, 20);

        assertTrue(response.getItems().isEmpty());
        assertEquals(0L, response.getTotal());
    }

    @Test
    void shouldGetIndexDetail() {
        IndexInfo index = createIndexInfo("000001", "上证指数", "宽基");
        index.setBaseDate(LocalDate.of(1991, 7, 15));
        index.setBasePoint(new BigDecimal("100.0000"));
        index.setComponentCount(1800);
        index.setPublishDate(LocalDate.of(1991, 7, 15));

        when(indexRepository.findByIndexCode("000001")).thenReturn(Optional.of(index));

        Optional<IndexDetailResponse> detail = indexService.getIndexDetail("000001");

        assertTrue(detail.isPresent());
        assertEquals("000001", detail.get().getIndexCode());
        assertEquals("上证指数", detail.get().getIndexName());
        assertEquals("宽基", detail.get().getIndexType());
        assertEquals(LocalDate.of(1991, 7, 15), detail.get().getBaseDate());
        assertEquals(new BigDecimal("100.0000"), detail.get().getBasePoint());
    }

    @Test
    void shouldReturnEmptyWhenIndexNotFound() {
        when(indexRepository.findByIndexCode("999999")).thenReturn(Optional.empty());

        Optional<IndexDetailResponse> detail = indexService.getIndexDetail("999999");

        assertTrue(detail.isEmpty());
    }

    @Test
    void shouldGetIndexTrend() {
        IndexHistory h1 = createIndexHistory("000001", LocalDate.of(2024, 1, 1), "day",
                new BigDecimal("2900"), new BigDecimal("3000"), new BigDecimal("2880"), new BigDecimal("2950"));
        IndexHistory h2 = createIndexHistory("000001", LocalDate.of(2024, 1, 2), "day",
                new BigDecimal("2950"), new BigDecimal("2980"), new BigDecimal("2920"), new BigDecimal("2960"));

        when(indexHistoryRepository.findByIndexCodeAndGranularity("000001", "day", null, null))
                .thenReturn(List.of(h1, h2));

        IndexTrendResponse response = indexService.getIndexTrend("000001", "day", null, null);

        assertEquals("000001", response.getIndexCode());
        assertEquals("day", response.getGranularity());
        assertEquals(2, response.getItems().size());
        assertEquals(new BigDecimal("2960"), response.getItems().get(1).getClosePrice());
    }

    @Test
    void shouldGetIndexTrendWithDateRange() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);
        IndexHistory h = createIndexHistory("000001", LocalDate.of(2024, 1, 15), "day",
                new BigDecimal("2900"), new BigDecimal("3000"), new BigDecimal("2880"), new BigDecimal("2950"));

        when(indexHistoryRepository.findByIndexCodeAndGranularity("000001", "day", start, end))
                .thenReturn(List.of(h));

        IndexTrendResponse response = indexService.getIndexTrend("000001", "day", start, end);

        assertEquals(1, response.getItems().size());
    }

    @Test
    void shouldGetIndexEtfsViaMapping() {
        IndexEtfMapping mapping1 = new IndexEtfMapping();
        mapping1.setEtfCode("510300");
        IndexEtfMapping mapping2 = new IndexEtfMapping();
        mapping2.setEtfCode("510330");

        EtfInfo etf1 = createEtfInfo("510300", "华泰柏瑞沪深300ETF", "000300");
        EtfInfo etf2 = createEtfInfo("510330", "华夏沪深300ETF", "000300");

        when(indexEtfMappingRepository.findByIndexCode("000300")).thenReturn(List.of(mapping1, mapping2));
        when(etfInfoRepository.findByEtfCodes(List.of("510300", "510330"))).thenReturn(List.of(etf1, etf2));

        List<EtfListItem> results = indexService.getIndexEtfs("000300");

        assertEquals(2, results.size());
        verify(etfInfoRepository).findByEtfCodes(List.of("510300", "510330"));
        verify(etfInfoRepository, never()).findByTrackingIndexCode(any());
    }

    @Test
    void shouldGetIndexEtfsViaFallbackWhenMappingEmpty() {
        EtfInfo etf = createEtfInfo("510300", "华泰柏瑞沪深300ETF", "000300");

        when(indexEtfMappingRepository.findByIndexCode("000300")).thenReturn(List.of());
        when(etfInfoRepository.findByTrackingIndexCode("000300")).thenReturn(List.of(etf));

        List<EtfListItem> results = indexService.getIndexEtfs("000300");

        assertEquals(1, results.size());
        assertEquals("510300", results.get(0).getEtfCode());
        verify(etfInfoRepository).findByTrackingIndexCode("000300");
    }

    @Test
    void shouldReturnEmptyEtfsWhenNoMappingAndNoTracking() {
        when(indexEtfMappingRepository.findByIndexCode("999999")).thenReturn(List.of());
        when(etfInfoRepository.findByTrackingIndexCode("999999")).thenReturn(List.of());

        List<EtfListItem> results = indexService.getIndexEtfs("999999");

        assertTrue(results.isEmpty());
    }

    @Test
    void shouldGetIndexCategories() {
        IndexInfo wide1 = createIndexInfo("000001", "上证指数", "宽基");
        wide1.setIsCore(true);
        IndexInfo wide2 = createIndexInfo("000300", "沪深300", "宽基");
        wide2.setIsCore(true);
        IndexInfo industry = createIndexInfo("399989", "中证医疗", "行业");
        industry.setIsCore(true);

        when(indexRepository.findCoreByType("宽基")).thenReturn(List.of(wide1, wide2));
        when(indexRepository.findCoreByType("行业")).thenReturn(List.of(industry));
        when(indexRepository.findCoreByType("主题")).thenReturn(List.of());
        when(indexRepository.findCoreByType("策略")).thenReturn(List.of());
        when(indexRepository.findCoreByType("其他")).thenReturn(List.of());

        List<IndexCategoryGroup> groups = indexService.getIndexCategories();

        assertEquals(2, groups.size());
        assertEquals("宽基", groups.get(0).getIndexType());
        assertEquals("宽基指数", groups.get(0).getIndexTypeLabel());
        assertEquals(2, groups.get(0).getItems().size());

        assertEquals("行业", groups.get(1).getIndexType());
        assertEquals("行业指数", groups.get(1).getIndexTypeLabel());
        assertEquals(1, groups.get(1).getItems().size());
    }

    @Test
    void shouldReturnEmptyCategoriesWhenNoCoreIndexes() {
        when(indexRepository.findCoreByType("宽基")).thenReturn(List.of());
        when(indexRepository.findCoreByType("行业")).thenReturn(List.of());
        when(indexRepository.findCoreByType("主题")).thenReturn(List.of());
        when(indexRepository.findCoreByType("策略")).thenReturn(List.of());
        when(indexRepository.findCoreByType("其他")).thenReturn(List.of());

        List<IndexCategoryGroup> groups = indexService.getIndexCategories();

        assertTrue(groups.isEmpty());
    }

    private IndexInfo createIndexInfo(String code, String name, String type) {
        IndexInfo idx = new IndexInfo();
        idx.setIndexCode(code);
        idx.setIndexName(name);
        idx.setIndexType(type);
        idx.setMarket("SH");
        idx.setIsCore(false);
        return idx;
    }

    private IndexHistory createIndexHistory(String code, LocalDate date, String granularity,
                                            BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close) {
        IndexHistory h = new IndexHistory();
        h.setIndexCode(code);
        h.setTradeDate(date);
        h.setGranularity(granularity);
        h.setOpenPrice(open);
        h.setHighPrice(high);
        h.setLowPrice(low);
        h.setClosePrice(close);
        h.setVolume(1000000L);
        h.setAmount(new BigDecimal("500000000"));
        return h;
    }

    private EtfInfo createEtfInfo(String code, String name, String trackingCode) {
        EtfInfo etf = new EtfInfo();
        etf.setEtfCode(code);
        etf.setEtfName(name);
        etf.setTrackingIndexCode(trackingCode);
        etf.setManagementFee(new BigDecimal("0.50"));
        etf.setFundSize(new BigDecimal("1000000000"));
        etf.setMarket("SH");
        return etf;
    }
}
