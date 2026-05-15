package org.cwowhappy.securityanalyze.financial.domain.repository;

import org.cwowhappy.securityanalyze.financial.domain.model.FinancialIndicator;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 财务指标领域仓库接口。
 */
public interface FinancialIndicatorRepository {

    void save(FinancialIndicator indicator);

    void saveAll(List<FinancialIndicator> indicators);

    List<FinancialIndicator> findByStockCode(String stockCode);

    List<FinancialIndicator> findByStockCode(String stockCode, String reportType);

    List<FinancialIndicator> findByStockCode(String stockCode, String reportType, int limit);

    Optional<FinancialIndicator> findLatest(String stockCode, String reportType);

    Optional<FinancialIndicator> findByStockCodeAndReportDate(String stockCode, LocalDate reportDate, String reportType);

    /**
     * 查询多只股票最新一期指标。
     */
    List<FinancialIndicator> findLatestByStockCodes(List<String> stockCodes, String reportType);
}
