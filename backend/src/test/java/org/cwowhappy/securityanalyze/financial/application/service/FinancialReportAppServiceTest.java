package org.cwowhappy.securityanalyze.financial.application.service;

import org.cwowhappy.securityanalyze.financial.application.dto.FinancialIncomeDTO;
import org.cwowhappy.securityanalyze.financial.application.dto.FinancialBalanceDTO;
import org.cwowhappy.securityanalyze.financial.application.dto.FinancialCashflowDTO;
import org.cwowhappy.securityanalyze.financial.domain.model.FinancialIncome;
import org.cwowhappy.securityanalyze.financial.domain.model.FinancialBalance;
import org.cwowhappy.securityanalyze.financial.domain.model.FinancialCashflow;
import org.cwowhappy.securityanalyze.financial.domain.repository.FinancialIncomeRepository;
import org.cwowhappy.securityanalyze.financial.domain.repository.FinancialBalanceRepository;
import org.cwowhappy.securityanalyze.financial.domain.repository.FinancialCashflowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialReportAppServiceTest {

    @Mock
    private FinancialIncomeRepository incomeRepository;
    @Mock
    private FinancialBalanceRepository balanceRepository;
    @Mock
    private FinancialCashflowRepository cashflowRepository;

    @InjectMocks
    private FinancialReportAppService reportAppService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void shouldReturnIncomeStatementsWithCalculatedMargins() {
        FinancialIncome income = FinancialIncome.builder()
                .stockCode("000001")
                .reportDate(LocalDate.of(2024, 12, 31))
                .reportType("Y")
                .revenue(new BigDecimal("100000000.00"))
                .operatingCost(new BigDecimal("60000000.00"))
                .grossProfit(new BigDecimal("40000000.00"))
                .netProfit(new BigDecimal("15000000.00"))
                .build();
        when(incomeRepository.findByStockCode("000001", "Y", 20)).thenReturn(List.of(income));

        List<FinancialIncomeDTO> result = reportAppService.getIncomeStatements("000001", "Y", 20);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getGrossMargin()).isEqualByComparingTo("40.00");
        assertThat(result.get(0).getNetMargin()).isEqualByComparingTo("15.00");
    }

    @Test
    void shouldReturnBalanceSheetsWithDebtRatio() {
        FinancialBalance balance = FinancialBalance.builder()
                .stockCode("000001")
                .reportDate(LocalDate.of(2024, 12, 31))
                .reportType("Y")
                .totalAssets(new BigDecimal("500000000.00"))
                .totalLiabilities(new BigDecimal("300000000.00"))
                .build();
        when(balanceRepository.findByStockCode("000001", "Y", 20)).thenReturn(List.of(balance));

        List<FinancialBalanceDTO> result = reportAppService.getBalanceSheets("000001", "Y", 20);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDebtRatio()).isEqualByComparingTo("60.00");
    }

    @Test
    void shouldReturnCashflowStatements() {
        FinancialCashflow cf = FinancialCashflow.builder()
                .stockCode("000001")
                .reportDate(LocalDate.of(2024, 12, 31))
                .reportType("Y")
                .cfOperating(new BigDecimal("50000000.00"))
                .build();
        when(cashflowRepository.findByStockCode("000001", "Y", 20)).thenReturn(List.of(cf));

        List<FinancialCashflowDTO> result = reportAppService.getCashflowStatements("000001", "Y", 20);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCfOperating()).isEqualByComparingTo("50000000.00");
    }

    @Test
    void shouldHandleNullRevenueWhenCalculatingMargins() {
        FinancialIncome income = FinancialIncome.builder()
                .stockCode("000001")
                .reportDate(LocalDate.of(2024, 12, 31))
                .reportType("Y")
                .revenue(null)
                .netProfit(new BigDecimal("15000000.00"))
                .build();
        when(incomeRepository.findByStockCode("000001", "Y", 20)).thenReturn(List.of(income));

        List<FinancialIncomeDTO> result = reportAppService.getIncomeStatements("000001", "Y", 20);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getGrossMargin()).isNull();
        assertThat(result.get(0).getNetMargin()).isNull();
    }
}
