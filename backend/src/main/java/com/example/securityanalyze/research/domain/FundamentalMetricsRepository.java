package com.example.securityanalyze.research.domain;

import java.util.List;
import java.util.Optional;

public interface FundamentalMetricsRepository {

    Optional<FundamentalMetrics> findByStockCode(String stockCode, int years);

    List<ScreenCompanyItem> screenCompanies(String keyword, String industry, String market, int offset, int limit);

    long countScreenCompanies(String keyword, String industry, String market);

    List<PeerMetric> findIndustryPeers(String stockCode);
}
