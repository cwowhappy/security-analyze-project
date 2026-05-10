package com.example.securityanalyze.industry.domain;

import java.util.List;

/**
 * 行业查询 Repository 接口
 */
public interface IndustryQueryRepository {

    List<IndustrySummary> findIndustries();

    List<IndustryCompany> findCompaniesByIndustry(String industry, int offset, int limit);

    long countCompaniesByIndustry(String industry);
}
