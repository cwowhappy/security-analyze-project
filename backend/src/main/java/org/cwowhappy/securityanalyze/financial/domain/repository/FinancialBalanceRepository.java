package org.cwowhappy.securityanalyze.financial.domain.repository;

import org.cwowhappy.securityanalyze.financial.domain.model.FinancialBalance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 资产负债表领域仓库接口。
 */
public interface FinancialBalanceRepository {

    void save(FinancialBalance balance);

    void saveAll(List<FinancialBalance> balances);

    List<FinancialBalance> findByStockCode(String stockCode);

    List<FinancialBalance> findByStockCode(String stockCode, String reportType);

    List<FinancialBalance> findByStockCode(String stockCode, String reportType, int limit);

    Optional<FinancialBalance> findLatest(String stockCode, String reportType);

    Optional<FinancialBalance> findByStockCodeAndReportDate(String stockCode, LocalDate reportDate, String reportType);
}
