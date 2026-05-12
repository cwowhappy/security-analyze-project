package org.cwowhappy.securityanalyze.company.domain.repository;

import org.cwowhappy.securityanalyze.company.domain.model.Company;
import org.cwowhappy.securityanalyze.company.domain.model.CompanyId;
import org.cwowhappy.securityanalyze.shared.dto.PageQuery;
import org.cwowhappy.securityanalyze.shared.dto.PageResult;

import java.util.List;
import java.util.Optional;

/**
 * 公司仓库接口（Port）。
 * 定义在领域层，由基础设施层实现。
 */
public interface CompanyRepository {

    Optional<Company> findById(CompanyId id);

    Optional<Company> findByUscCode(String uscCode);

    PageResult<Company> findByPage(PageQuery pageQuery);

    PageResult<Company> findByPage(PageQuery pageQuery, String industry, String province, String controllerType, String keyword);

    List<Company> findByIndustry(String industry);

    List<Company> findByNameLike(String keyword);

    CompanyId save(Company company);

    void deleteById(CompanyId id);
}
