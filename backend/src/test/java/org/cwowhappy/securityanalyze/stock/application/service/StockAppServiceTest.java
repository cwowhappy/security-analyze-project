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

import org.cwowhappy.securityanalyze.shared.dto.PageQuery;
import org.cwowhappy.securityanalyze.shared.dto.PageResult;

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
                .stockCode("000001")
                .name("平安银行")
                .market("SZ")
                .industry("银行")
                .build();
    }

    @Test
    void shouldReturnStockWhenFoundByStockCode() {
        // Arrange
        when(stockRepository.findByStockCode("000001")).thenReturn(Optional.of(sampleStock));

        // Act
        Optional<StockDTO> result = stockAppService.findByStockCode("000001");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getStockCode()).isEqualTo("000001");
        assertThat(result.get().getName()).isEqualTo("平安银行");
        verify(stockRepository, times(1)).findByStockCode("000001");
    }

    @Test
    void shouldReturnEmptyWhenStockNotFoundByStockCode() {
        // Arrange
        when(stockRepository.findByStockCode("999999")).thenReturn(Optional.empty());

        // Act
        Optional<StockDTO> result = stockAppService.findByStockCode("999999");

        // Assert
        assertThat(result).isEmpty();
        verify(stockRepository, times(1)).findByStockCode("999999");
    }

    @Test
    void shouldCreateStockAndReturnId() {
        // Arrange
        StockDTO dto = StockDTO.builder()
                .stockCode("000002")
                .name("万科A")
                .market("SZ")
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

    @Test
    void shouldReturnPageResultWhenFindByPage() {
        // Arrange
        PageQuery query = new PageQuery();
        query.setPage(1);
        query.setSize(20);
        PageResult<Stock> pageResult = PageResult.<Stock>builder()
                .list(List.of(sampleStock))
                .total(1L)
                .page(1)
                .size(20)
                .build();
        when(stockRepository.findByPage(query, "主板", "银行", "深圳", "平安"))
                .thenReturn(pageResult);

        // Act
        PageResult<StockDTO> result = stockAppService.findByPage(query, "主板", "银行", "深圳", "平安");

        // Assert
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getTotal()).isEqualTo(1L);
        verify(stockRepository, times(1)).findByPage(query, "主板", "银行", "深圳", "平安");
    }

    @Test
    void shouldReturnStocksWhenFindByCompanyId() {
        // Arrange
        when(stockRepository.findByCompanyId("comp001")).thenReturn(List.of(sampleStock));

        // Act
        List<StockDTO> result = stockAppService.findByCompanyId("comp001");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStockCode()).isEqualTo("000001");
        verify(stockRepository, times(1)).findByCompanyId("comp001");
    }
}
