package org.cwowhappy.securityanalyze.financial.application.service;

import org.cwowhappy.securityanalyze.financial.application.dto.DupontAnalysisDTO;
import org.cwowhappy.securityanalyze.financial.application.dto.TrendDataDTO;
import org.cwowhappy.securityanalyze.financial.domain.model.FinancialBalance;
import org.cwowhappy.securityanalyze.financial.domain.model.FinancialIncome;
import org.cwowhappy.securityanalyze.financial.domain.model.FinancialIndicator;
import org.cwowhappy.securityanalyze.financial.domain.repository.FinancialBalanceRepository;
import org.cwowhappy.securityanalyze.financial.domain.repository.FinancialIncomeRepository;
import org.cwowhappy.securityanalyze.financial.domain.repository.FinancialIndicatorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialAnalysisAppServiceTest {

    @Mock
    private FinancialIndicatorRepository indicatorRepository;
    @Mock
    private FinancialIncomeRepository incomeRepository;
    @Mock
    private FinancialBalanceRepository balanceRepository;

    @InjectMocks
    private FinancialAnalysisAppService analysisAppService;

    @Test
    void shouldReturnTrendData() {
        FinancialIndicator i1 = FinancialIndicator.builder()
                .stockCode("000001")
                .reportDate(LocalDate.of(2024, 12, 31))
                .roe(new BigDecimal("12.50"))
                .grossMargin(new BigDecimal("35.00"))
                .build();
        FinancialIndicator i2 = FinancialIndicator.builder()
                .stockCode("000001")
                .reportDate(LocalDate.of(2023, 12, 31))
                .roe(new BigDecimal("11.00"))
                .grossMargin(new BigDecimal("34.00"))
                .build();
        when(indicatorRepository.findByStockCode("000001", "Y", 8)).thenReturn(List.of(i1, i2));

        List<TrendDataDTO> result = analysisAppService.getTrend("000001", List.of("roe", "grossMargin"), "Y", 8);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getData()).hasSize(2);
        assertThat(result.get(0).getData().get(0).getValue()).isEqualByComparingTo("12.50");
    }

    @Test
    void shouldReturnDupontAnalysis() {
        FinancialIncome income = FinancialIncome.builder()
                .stockCode("000001")
                .reportDate(LocalDate.of(2024, 12, 31))
                .reportType("Y")
                .revenue(new BigDecimal("100000000.00"))
                .netProfit(new BigDecimal("15000000.00"))
                .build();
        FinancialBalance balance = FinancialBalance.builder()
                .stockCode("000001")
                .reportDate(LocalDate.of(2024, 12, 31))
                .reportType("Y")
                .totalAssets(new BigDecimal("500000000.00"))
                .totalEquity(new BigDecimal("200000000.00"))
                .build();
        when(incomeRepository.findByStockCodeAndReportDate("000001", LocalDate.of(2024, 12, 31), "Y"))
                .thenReturn(Optional.of(income));
        when(balanceRepository.findByStockCodeAndReportDate("000001", LocalDate.of(2024, 12, 31), "Y"))
                .thenReturn(Optional.of(balance));

        DupontAnalysisDTO result = analysisAppService.getDupontAnalysis("000001", LocalDate.of(2024, 12, 31), "Y");

        assertThat(result).isNotNull();
        assertThat(result.getNetMargin()).isEqualByComparingTo("15.00");
        assertThat(result.getAssetTurnover()).isEqualByComparingTo("0.2000");
        assertThat(result.getEquityMultiplier()).isEqualByComparingTo("2.5000");
        assertThat(result.getRoe()).isEqualByComparingTo("7.50");
    }

    @Test
    void shouldReturnNullDupontWhenDataMissing() {
        when(incomeRepository.findByStockCodeAndReportDate("000001", LocalDate.of(2024, 12, 31), "Y"))
                .thenReturn(Optional.empty());

        DupontAnalysisDTO result = analysisAppService.getDupontAnalysis("000001", LocalDate.of(2024, 12, 31), "Y");

        assertThat(result).isNull();
    }
}
