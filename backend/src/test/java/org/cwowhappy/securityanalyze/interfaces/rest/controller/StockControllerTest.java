package org.cwowhappy.securityanalyze.interfaces.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cwowhappy.securityanalyze.interfaces.rest.request.CreateStockRequest;
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

    @Test
    void shouldReturnStocksWhenListStocks() throws Exception {
        // Arrange
        StockDTO dto = StockDTO.builder()
                .id("stk001")
                .stockCode("000001")
                .name("平安银行")
                .build();
        when(stockAppService.findAll()).thenReturn(List.of(dto));

        // Act & Assert
        mockMvc.perform(get("/api/stocks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].stockCode").value("000001"));
    }

    @Test
    void shouldReturnStockWhenFoundByStockCode() throws Exception {
        // Arrange
        StockDTO dto = StockDTO.builder()
                .id("stk001")
                .stockCode("000001")
                .name("平安银行")
                .build();
        when(stockAppService.findByStockCode("000001")).thenReturn(Optional.of(dto));

        // Act & Assert
        mockMvc.perform(get("/api/stocks/000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.stockCode").value("000001"));
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
        mockMvc.perform(post("/api/stocks")
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
        mockMvc.perform(post("/api/stocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
