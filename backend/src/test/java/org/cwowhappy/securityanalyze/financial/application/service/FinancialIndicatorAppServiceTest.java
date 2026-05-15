package org.cwowhappy.securityanalyze.financial.application.service;

import org.cwowhappy.securityanalyze.financial.application.dto.FinancialIndicatorDTO;
import org.cwowhappy.securityanalyze.financial.domain.model.FinancialIndicator;
import org.cwowhappy.securityanalyze.financial.domain.repository.FinancialIndicatorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialIndicatorAppServiceTest {

    @Mock
    private FinancialIndicatorRepository indicatorRepository;

    @InjectMocks
    private FinancialIndicatorAppService indicatorAppService;

    @Test
    void shouldReturnIndicators() {
        FinancialIndicator indicator = FinancialIndicator.builder()
                .stockCode("000001")
                .reportDate(LocalDate.of(2024, 12, 31))
                .reportType("Y")
                .roe(new BigDecimal("12.50"))
                .grossMargin(new BigDecimal("35.00"))
                .build();
        when(indicatorRepository.findByStockCode("000001", "Y", 20)).thenReturn(List.of(indicator));

        List<FinancialIndicatorDTO> result = indicatorAppService.getIndicators("000001", "Y", 20);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRoe()).isEqualByComparingTo("12.50");
        assertThat(result.get(0).getGrossMargin()).isEqualByComparingTo("35.00");
    }

    @Test
    void shouldReturnEmptyListWhenNoIndicators() {
        when(indicatorRepository.findByStockCode("999999", "Y", 20)).thenReturn(List.of());

        List<FinancialIndicatorDTO> result = indicatorAppService.getIndicators("999999", "Y", 20);

        assertThat(result).isEmpty();
    }
}
