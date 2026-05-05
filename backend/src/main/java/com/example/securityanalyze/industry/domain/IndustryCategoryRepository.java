package com.example.securityanalyze.industry.domain;

import java.util.List;
import java.util.Optional;

/**
 * 行业分类维度 Repository 接口
 */
public interface IndustryCategoryRepository {

    void syncCategories(List<IndustryCategory> categories);

    List<IndustryCategory> findByStandard(String standardCode);

    List<IndustryCategory> findByStandardAndLevel(String standardCode, int level);

    List<IndustryCategory> findByStandardAndParent(String standardCode, String parentCode);

    Optional<IndustryCategory> findByCode(String standardCode, String code);

    List<IndustryCategory> findByStandardAndLevelWithCount(String standardCode, int level);
}
