package com.example.securityanalyze.research.domain;

import java.util.List;
import java.util.Optional;

public interface StockFundamentalMetricsRepository {

    Optional<StockFundamentalMetrics> findByStockCodeAndYear(String stockCode, int reportYear);

    List<StockFundamentalMetrics> findByStockCode(String stockCode, int limit);

    void batchUpsert(List<StockFundamentalMetrics> metrics);

    void deleteByStockCodeAndYear(String stockCode, int reportYear);
}
