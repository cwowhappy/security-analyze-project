package org.cwowhappy.securityanalyze.financial.domain.repository;

import org.cwowhappy.securityanalyze.financial.domain.model.FinancialIncome;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 利润表领域仓库接口。
 */
public interface FinancialIncomeRepository {

    void save(FinancialIncome income);

    void saveAll(List<FinancialIncome> incomes);

    List<FinancialIncome> findByStockCode(String stockCode);

    List<FinancialIncome> findByStockCode(String stockCode, String reportType);

    List<FinancialIncome> findByStockCode(String stockCode, String reportType, int limit);

    Optional<FinancialIncome> findLatest(String stockCode, String reportType);

    Optional<FinancialIncome> findByStockCodeAndReportDate(String stockCode, LocalDate reportDate, String reportType);
}
