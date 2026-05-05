package com.example.securityanalyze.index.api;

import com.example.securityanalyze.index.application.IndexService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class IndexControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IndexService indexService;

    @Test
    void shouldListIndexes() throws Exception {
        IndexListItem item = new IndexListItem();
        item.setIndexCode("000001");
        item.setIndexName("上证指数");
        item.setIndexType("宽基");
        item.setMarket("SH");
        item.setPublishDate(LocalDate.of(1991, 7, 15));

        IndexListResponse response = new IndexListResponse();
        response.setItems(List.of(item));
        response.setTotal(1L);
        response.setPage(0);
        response.setSize(20);

        when(indexService.listIndexes(null, 0, 20)).thenReturn(response);

        mockMvc.perform(get("/api/indexes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].indexCode").value("000001"))
                .andExpect(jsonPath("$.items[0].indexName").value("上证指数"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void shouldListIndexesWithKeyword() throws Exception {
        IndexListItem item = new IndexListItem();
        item.setIndexCode("000300");
        item.setIndexName("沪深300");

        IndexListResponse response = new IndexListResponse();
        response.setItems(List.of(item));
        response.setTotal(1L);
        response.setPage(0);
        response.setSize(20);

        when(indexService.listIndexes("沪深", 0, 20)).thenReturn(response);

        mockMvc.perform(get("/api/indexes?keyword=沪深"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].indexCode").value("000300"));
    }

    @Test
    void shouldGetIndexDetail() throws Exception {
        IndexDetailResponse detail = new IndexDetailResponse();
        detail.setIndexCode("000001");
        detail.setIndexName("上证指数");
        detail.setIndexType("宽基");
        detail.setMarket("SH");
        detail.setBaseDate(LocalDate.of(1991, 7, 15));
        detail.setBasePoint(new BigDecimal("100.0000"));
        detail.setComponentCount(1800);

        when(indexService.getIndexDetail("000001")).thenReturn(Optional.of(detail));

        mockMvc.perform(get("/api/indexes/000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indexCode").value("000001"))
                .andExpect(jsonPath("$.indexName").value("上证指数"))
                .andExpect(jsonPath("$.basePoint").value(100.0000));
    }

    @Test
    void shouldReturn404WhenIndexNotFound() throws Exception {
        when(indexService.getIndexDetail("999999")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/indexes/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetIndexTrend() throws Exception {
        IndexTrendItem item1 = new IndexTrendItem();
        item1.setTradeDate(LocalDate.of(2024, 1, 1));
        item1.setClosePrice(new BigDecimal("2950.00"));
        item1.setVolume(1000000L);

        IndexTrendItem item2 = new IndexTrendItem();
        item2.setTradeDate(LocalDate.of(2024, 1, 2));
        item2.setClosePrice(new BigDecimal("2960.00"));
        item2.setVolume(1200000L);

        IndexTrendResponse response = new IndexTrendResponse();
        response.setIndexCode("000001");
        response.setGranularity("day");
        response.setItems(List.of(item1, item2));

        when(indexService.getIndexTrend("000001", "day", null, null)).thenReturn(response);

        mockMvc.perform(get("/api/indexes/000001/trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indexCode").value("000001"))
                .andExpect(jsonPath("$.granularity").value("day"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].closePrice").value(2950.00));
    }

    @Test
    void shouldGetIndexTrendWithDateRange() throws Exception {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);

        IndexTrendResponse response = new IndexTrendResponse();
        response.setIndexCode("000001");
        response.setGranularity("week");
        response.setItems(List.of());

        when(indexService.getIndexTrend("000001", "week", start, end)).thenReturn(response);

        mockMvc.perform(get("/api/indexes/000001/trend?granularity=week&startDate=2024-01-01&endDate=2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.granularity").value("week"));
    }

    @Test
    void shouldGetIndexEtfs() throws Exception {
        EtfListItem etf1 = new EtfListItem();
        etf1.setEtfCode("510300");
        etf1.setEtfName("华泰柏瑞沪深300ETF");
        etf1.setTrackingIndexCode("000300");
        etf1.setFundSize(new BigDecimal("1000000000"));

        EtfListItem etf2 = new EtfListItem();
        etf2.setEtfCode("510330");
        etf2.setEtfName("华夏沪深300ETF");
        etf2.setTrackingIndexCode("000300");
        etf2.setFundSize(new BigDecimal("500000000"));

        when(indexService.getIndexEtfs("000300")).thenReturn(List.of(etf1, etf2));

        mockMvc.perform(get("/api/indexes/000300/etfs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].etfCode").value("510300"))
                .andExpect(jsonPath("$[1].etfCode").value("510330"));
    }

    @Test
    void shouldGetIndexCategories() throws Exception {
        IndexCategoryGroup wideGroup = new IndexCategoryGroup();
        wideGroup.setIndexType("宽基");
        wideGroup.setIndexTypeLabel("宽基指数");

        IndexListItem item1 = new IndexListItem();
        item1.setIndexCode("000001");
        item1.setIndexName("上证指数");
        IndexListItem item2 = new IndexListItem();
        item2.setIndexCode("000300");
        item2.setIndexName("沪深300");
        wideGroup.setItems(List.of(item1, item2));

        IndexCategoryGroup industryGroup = new IndexCategoryGroup();
        industryGroup.setIndexType("行业");
        industryGroup.setIndexTypeLabel("行业指数");
        industryGroup.setItems(List.of());

        when(indexService.getIndexCategories()).thenReturn(List.of(wideGroup, industryGroup));

        mockMvc.perform(get("/api/indexes/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].indexType").value("宽基"))
                .andExpect(jsonPath("$[0].items.length()").value(2))
                .andExpect(jsonPath("$[1].indexTypeLabel").value("行业指数"));
    }

    @Test
    void shouldNormalizePaginationParams() throws Exception {
        IndexListResponse response = new IndexListResponse();
        response.setItems(List.of());
        response.setTotal(0L);
        response.setPage(0);
        response.setSize(20);

        when(indexService.listIndexes(null, 0, 20)).thenReturn(response);

        mockMvc.perform(get("/api/indexes?page=-1&size=0"))
                .andExpect(status().isOk());
    }
}
