package com.example.securityanalyze.industry.domain;

import java.util.List;

/**
 * 公司与行业分类映射 Repository 接口
 */
public interface CompanyIndustryMappingRepository {

    void save(CompanyIndustryMapping mapping);

    void saveAll(List<CompanyIndustryMapping> mappings);

    List<CompanyIndustryMapping> findByCompanyId(Long companyId);

    List<CompanyIndustryMapping> findByCompanyIdAndStandard(Long companyId, String standardCode);

    List<Long> findCompanyIdsByStandardAndLevel1(String standardCode, String level1Code);

    List<Long> findCompanyIdsByStandardAndLevel2(String standardCode, String level2Code);

    long countByStandardAndLevel1(String standardCode, String level1Code);

    long countByStandardAndLevel2(String standardCode, String level2Code);

    void deleteByCompanyId(Long companyId);
}
