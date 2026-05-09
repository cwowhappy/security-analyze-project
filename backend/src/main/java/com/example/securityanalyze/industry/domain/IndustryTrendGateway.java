package com.example.securityanalyze.industry.domain;

import java.util.List;

/**
 * 行业趋势数据源网关接口
 */
public interface IndustryTrendGateway {

    /**
     * 获取行业趋势数据
     *
     * @param industryName 行业名称
     * @param period       周期（1m/3m/6m/1y）
     * @return 趋势数据点列表，失败时返回空列表
     */
    List<IndustryTrendPoint> fetchTrend(String industryName, String period);
}
