package org.cwowhappy.securityanalyze.interfaces.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cwowhappy.securityanalyze.company.application.dto.CompanyDTO;
import org.cwowhappy.securityanalyze.company.application.service.CompanyAppService;
import org.cwowhappy.securityanalyze.interfaces.rest.request.CreateStockRequest;
import org.cwowhappy.securityanalyze.shared.dto.PageQuery;
import org.cwowhappy.securityanalyze.shared.dto.PageResult;
import org.cwowhappy.securityanalyze.stock.application.dto.StockDTO;
import org.cwowhappy.securityanalyze.stock.application.service.StockAppService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * StockController Web 层测试（@WebMvcTest，只加载 Controller 层）。
 */
@WebMvcTest(StockController.class)
class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StockAppService stockAppService;

    @MockitoBean
    private CompanyAppService companyAppService;

    @Test
    void shouldReturnStocksWhenListStocks() throws Exception {
        // Arrange
        StockDTO dto = StockDTO.builder()
                .id("stk001")
                .stockCode("000001")
                .name("平安银行")
                .build();
        PageResult<StockDTO> pageResult = PageResult.<StockDTO>builder()
                .list(List.of(dto))
                .total(1)
                .page(1)
                .size(20)
                .build();
        when(stockAppService.findByPage(any(PageQuery.class), eq(null), eq(null), eq(null), eq(null)))
                .thenReturn(pageResult);

        // Act & Assert
        mockMvc.perform(get("/api/v1/stocks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.list[0].stockCode").value("000001"));
    }

    @Test
    void shouldReturnStockWhenFoundByStockCode() throws Exception {
        // Arrange
        StockDTO dto = StockDTO.builder()
                .id("stk001")
                .stockCode("000001")
                .name("平安银行")
                .companyId("comp001")
                .build();
        CompanyDTO company = CompanyDTO.builder()
                .id("comp001")
                .name("平安银行股份有限公司")
                .build();
        when(stockAppService.findByStockCode("000001")).thenReturn(Optional.of(dto));
        when(companyAppService.findById("comp001")).thenReturn(Optional.of(company));

        // Act & Assert
        mockMvc.perform(get("/api/v1/stocks/000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.stockCode").value("000001"))
                .andExpect(jsonPath("$.data.company.name").value("平安银行股份有限公司"));
    }

    @Test
    void shouldCreateStockWhenRequestValid() throws Exception {
        // Arrange
        CreateStockRequest request = new CreateStockRequest();
        request.setStockCode("000002");
        request.setName("万科A");
        request.setMarket("SZ");

        when(stockAppService.createStock(any(StockDTO.class))).thenReturn("stk002");

        // Act & Assert
        mockMvc.perform(post("/api/v1/stocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("stk002"));
    }

    @Test
    void shouldReturnBadRequestWhenStockCodeBlank() throws Exception {
        // Arrange
        CreateStockRequest request = new CreateStockRequest();
        request.setStockCode("");
        request.setName("万科A");

        // Act & Assert
        mockMvc.perform(post("/api/v1/stocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
