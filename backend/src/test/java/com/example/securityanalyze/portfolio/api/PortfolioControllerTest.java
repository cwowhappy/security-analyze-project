package com.example.securityanalyze.portfolio.api;

import com.example.securityanalyze.portfolio.application.PortfolioService;
import com.example.securityanalyze.portfolio.domain.Portfolio;
import com.example.securityanalyze.portfolio.domain.PortfolioType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PortfolioController 单元测试。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "testuser")
class PortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PortfolioService portfolioService;

    @Test
    void shouldListPortfolios() throws Exception {
        Portfolio p1 = portfolio(1L, "组合A", PortfolioType.REAL);
        Portfolio p2 = portfolio(2L, "组合B", PortfolioType.SIMULATION);
        when(portfolioService.listPortfolios("testuser")).thenReturn(List.of(p1, p2));

        mockMvc.perform(get("/api/portfolios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("组合A"))
                .andExpect(jsonPath("$[0].type").value("REAL"))
                .andExpect(jsonPath("$[1].name").value("组合B"))
                .andExpect(jsonPath("$[1].type").value("SIMULATION"));
    }

    @Test
    void shouldCreatePortfolio() throws Exception {
        Portfolio created = portfolio(1L, "新建组合", PortfolioType.REAL);
        created.setBroker("华泰证券");
        created.setDescription("测试描述");
        when(portfolioService.createPortfolio("testuser", "新建组合", PortfolioType.REAL, "华泰证券", "测试描述"))
                .thenReturn(created);

        mockMvc.perform(post("/api/portfolios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"新建组合","type":"REAL","broker":"华泰证券","description":"测试描述"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("新建组合"))
                .andExpect(jsonPath("$.type").value("REAL"))
                .andExpect(jsonPath("$.broker").value("华泰证券"));
    }

    @Test
    void shouldUpdatePortfolio() throws Exception {
        Portfolio updated = portfolio(1L, "更新后名称", PortfolioType.REAL);
        updated.setBroker("中信证券");
        when(portfolioService.getPortfolio("testuser", 1L)).thenReturn(updated);

        mockMvc.perform(put("/api/portfolios/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"更新后名称","type":"REAL","broker":"中信证券"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("更新后名称"))
                .andExpect(jsonPath("$.broker").value("中信证券"));
    }

    @Test
    void shouldDeletePortfolio() throws Exception {
        doNothing().when(portfolioService).deletePortfolio("testuser", 1L);

        mockMvc.perform(delete("/api/portfolios/1"))
                .andExpect(status().isNoContent());
    }

    private Portfolio portfolio(Long id, String name, PortfolioType type) {
        Portfolio p = new Portfolio();
        p.setId(id);
        p.setName(name);
        p.setType(type);
        p.setCreatedAt(LocalDateTime.now());
        return p;
    }
}
