package org.cwowhappy.securityanalyze.stock.application.service;

import org.cwowhappy.securityanalyze.stock.application.dto.StockDTO;
import org.cwowhappy.securityanalyze.stock.application.service.impl.StockAppServiceImpl;
import org.cwowhappy.securityanalyze.stock.domain.model.Stock;
import org.cwowhappy.securityanalyze.stock.domain.model.StockId;
import org.cwowhappy.securityanalyze.stock.domain.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 股票应用服务单元测试（纯 Mockito，不启动 Spring 上下文）。
 */
@ExtendWith(MockitoExtension.class)
class StockAppServiceTest {

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private StockAppServiceImpl stockAppService;

    private Stock sampleStock;

    @BeforeEach
    void setUp() {
        sampleStock = Stock.builder()
                .id(StockId.of("stk001"))
                .symbol("000001")
                .name("平安银行")
                .market("SZ")
                .currentPrice(new BigDecimal("12.50"))
                .changePercent(new BigDecimal("1.23"))
                .build();
    }

    @Test
    void shouldReturnStockWhenFoundBySymbol() {
        // Arrange
        when(stockRepository.findBySymbol("000001")).thenReturn(Optional.of(sampleStock));

        // Act
        Optional<StockDTO> result = stockAppService.findBySymbol("000001");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getSymbol()).isEqualTo("000001");
        assertThat(result.get().getName()).isEqualTo("平安银行");
        verify(stockRepository, times(1)).findBySymbol("000001");
    }

    @Test
    void shouldReturnEmptyWhenStockNotFoundBySymbol() {
        // Arrange
        when(stockRepository.findBySymbol("999999")).thenReturn(Optional.empty());

        // Act
        Optional<StockDTO> result = stockAppService.findBySymbol("999999");

        // Assert
        assertThat(result).isEmpty();
        verify(stockRepository, times(1)).findBySymbol("999999");
    }

    @Test
    void shouldReturnAllStocks() {
        // Arrange
        when(stockRepository.findAll()).thenReturn(List.of(sampleStock));

        // Act
        List<StockDTO> result = stockAppService.findAll();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSymbol()).isEqualTo("000001");
    }

    @Test
    void shouldCreateStockAndReturnId() {
        // Arrange
        StockDTO dto = StockDTO.builder()
                .symbol("000002")
                .name("万科A")
                .market("SZ")
                .currentPrice(new BigDecimal("15.00"))
                .build();
        when(stockRepository.save(any(Stock.class))).thenAnswer(inv -> {
            Stock stock = inv.getArgument(0);
            return stock.getId();
        });

        // Act
        String id = stockAppService.createStock(dto);

        // Assert
        assertThat(id).isNotNull();
        verify(stockRepository, times(1)).save(any(Stock.class));
    }
}
