package com.example.securityanalyze.research.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 估值指标 Repository 接口
 */
public interface ValuationMetricsRepository {

    /**
     * 查询某股票最新的估值指标
     */
    Optional<ValuationMetrics> findLatestByStockCode(String stockCode);

    /**
     * 查询某股票指定日期范围内的估值指标序列
     */
    List<ValuationMetrics> findHistoryByStockCode(String stockCode, LocalDate startDate, LocalDate endDate);

    /**
     * 查询某股票某指标在近N年内的统计值（最小、最大、中位数）
     */
    MetricStats findMetricStats(String stockCode, String metricName, int years);

    /**
     * 查询公司基本信息（名称、行业、市场）
     */
    CompanyBasicInfo findCompanyBasicInfo(String stockCode);

    /**
     * 查询某股票最近年报的经营现金流（用于 DCF）
     */
    BigDecimal findLatestOperatingCashFlow(String stockCode);

    /**
     * 查询某股票阶段B的衍生指标（用于综合评分）
     */
    Optional<StockFundamentalMetrics> findLatestFundamentalMetrics(String stockCode);
}
