package org.cwowhappy.securityanalyze.financial.application.service;

import lombok.RequiredArgsConstructor;
import org.cwowhappy.securityanalyze.financial.application.dto.FinancialBalanceDTO;
import org.cwowhappy.securityanalyze.financial.application.dto.FinancialCashflowDTO;
import org.cwowhappy.securityanalyze.financial.application.dto.FinancialIncomeDTO;
import org.cwowhappy.securityanalyze.financial.domain.model.FinancialBalance;
import org.cwowhappy.securityanalyze.financial.domain.model.FinancialCashflow;
import org.cwowhappy.securityanalyze.financial.domain.model.FinancialIncome;
import org.cwowhappy.securityanalyze.financial.domain.repository.FinancialBalanceRepository;
import org.cwowhappy.securityanalyze.financial.domain.repository.FinancialCashflowRepository;
import org.cwowhappy.securityanalyze.financial.domain.repository.FinancialIncomeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 财报查询应用服务。
 */
@Service
@RequiredArgsConstructor
public class FinancialReportAppService {

    private final FinancialIncomeRepository incomeRepository;
    private final FinancialBalanceRepository balanceRepository;
    private final FinancialCashflowRepository cashflowRepository;

    public List<FinancialIncomeDTO> getIncomeStatements(String stockCode, String reportType, int limit) {
        List<FinancialIncome> incomes = incomeRepository.findByStockCode(stockCode, reportType, limit);
        return incomes.stream().map(this::toIncomeDTO).toList();
    }

    public List<FinancialBalanceDTO> getBalanceSheets(String stockCode, String reportType, int limit) {
        List<FinancialBalance> balances = balanceRepository.findByStockCode(stockCode, reportType, limit);
        return balances.stream().map(this::toBalanceDTO).toList();
    }

    public List<FinancialCashflowDTO> getCashflowStatements(String stockCode, String reportType, int limit) {
        List<FinancialCashflow> cashflows = cashflowRepository.findByStockCode(stockCode, reportType, limit);
        return cashflows.stream().map(this::toCashflowDTO).toList();
    }

    private FinancialIncomeDTO toIncomeDTO(FinancialIncome income) {
        BigDecimal grossMargin = calcPercentage(income.getGrossProfit(), income.getRevenue());
        BigDecimal netMargin = calcPercentage(income.getNetProfit(), income.getRevenue());

        return FinancialIncomeDTO.builder()
            .stockCode(income.getStockCode())
            .reportDate(income.getReportDate())
            .reportType(income.getReportType())
            .basicEps(income.getBasicEps())
            .dilutedEps(income.getDilutedEps())
            .totalRevenue(income.getTotalRevenue())
            .revenue(income.getRevenue())
            .operatingCost(income.getOperatingCost())
            .grossProfit(income.getGrossProfit())
            .grossMargin(grossMargin)
            .sellingExpense(income.getSellingExpense())
            .adminExpense(income.getAdminExpense())
            .rdExpense(income.getRdExpense())
            .financialExpense(income.getFinancialExpense())
            .operatingProfit(income.getOperatingProfit())
            .totalProfit(income.getTotalProfit())
            .netProfit(income.getNetProfit())
            .npParentCompany(income.getNpParentCompany())
            .npExclNonrecurring(income.getNpExclNonrecurring())
            .netMargin(netMargin)
            .build();
    }

    private FinancialBalanceDTO toBalanceDTO(FinancialBalance balance) {
        BigDecimal debtRatio = calcPercentage(balance.getTotalLiabilities(), balance.getTotalAssets());

        return FinancialBalanceDTO.builder()
            .stockCode(balance.getStockCode())
            .reportDate(balance.getReportDate())
            .reportType(balance.getReportType())
            .totalAssets(balance.getTotalAssets())
            .totalLiabilities(balance.getTotalLiabilities())
            .totalEquity(balance.getTotalEquity())
            .equityParentCompany(balance.getEquityParentCompany())
            .currentAssets(balance.getCurrentAssets())
            .nonCurrentAssets(balance.getNonCurrentAssets())
            .cashEquivalents(balance.getCashEquivalents())
            .accountsReceivable(balance.getAccountsReceivable())
            .inventories(balance.getInventories())
            .currentLiabilities(balance.getCurrentLiabilities())
            .nonCurrentLiabilities(balance.getNonCurrentLiabilities())
            .accountsPayable(balance.getAccountsPayable())
            .shortTermBorrowings(balance.getShortTermBorrowings())
            .longTermBorrowings(balance.getLongTermBorrowings())
            .goodwill(balance.getGoodwill())
            .debtRatio(debtRatio)
            .build();
    }

    private FinancialCashflowDTO toCashflowDTO(FinancialCashflow cashflow) {
        return FinancialCashflowDTO.builder()
            .stockCode(cashflow.getStockCode())
            .reportDate(cashflow.getReportDate())
            .reportType(cashflow.getReportType())
            .cfOperating(cashflow.getCfOperating())
            .cfInvesting(cashflow.getCfInvesting())
            .cfFinancing(cashflow.getCfFinancing())
            .netCashFlow(cashflow.getNetCashFlow())
            .freeCashFlow(cashflow.getFreeCashFlow())
            .capex(cashflow.getCapex())
            .cashReceivedOperating(cashflow.getCashReceivedOperating())
            .taxPaid(cashflow.getTaxPaid())
            .build();
    }

    private BigDecimal calcPercentage(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return numerator.divide(denominator, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .setScale(2, RoundingMode.HALF_UP);
    }
}
