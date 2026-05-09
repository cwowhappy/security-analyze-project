package com.example.securityanalyze.research.domain;

import java.util.List;
import java.util.Optional;

public interface FundamentalMetricsRepository {

    Optional<FundamentalMetrics> findByStockCode(String stockCode, int years);

    List<ScreenCompanyItem> screenCompanies(String keyword, String industry, String market, int offset, int limit);

    long countScreenCompanies(String keyword, String industry, String market);

    List<PeerMetric> findIndustryPeers(String stockCode);

    /**
     * 根据股票代码查询所属行业
     *
     * @param stockCode 股票代码
     * @return 行业名称，不存在时返回 null
     */
    String findIndustryByStockCode(String stockCode);

    /**
     * 查询指定行业所有公司的最新年度指标（用于行业排名）
     *
     * @param industry 行业名称
     * @return 行业排名条目列表
     */
    List<IndustryRankItem> findIndustryRankItems(String industry);
}
