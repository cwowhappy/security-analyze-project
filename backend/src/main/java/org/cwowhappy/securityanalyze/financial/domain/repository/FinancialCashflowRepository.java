package org.cwowhappy.securityanalyze.financial.domain.repository;

import org.cwowhappy.securityanalyze.financial.domain.model.FinancialCashflow;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 现金流量表领域仓库接口。
 */
public interface FinancialCashflowRepository {

    void save(FinancialCashflow cashflow);

    void saveAll(List<FinancialCashflow> cashflows);

    List<FinancialCashflow> findByStockCode(String stockCode);

    List<FinancialCashflow> findByStockCode(String stockCode, String reportType);

    List<FinancialCashflow> findByStockCode(String stockCode, String reportType, int limit);

    Optional<FinancialCashflow> findLatest(String stockCode, String reportType);

    Optional<FinancialCashflow> findByStockCodeAndReportDate(String stockCode, LocalDate reportDate, String reportType);
}
