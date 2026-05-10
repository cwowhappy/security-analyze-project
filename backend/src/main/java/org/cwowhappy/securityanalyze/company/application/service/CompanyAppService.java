package org.cwowhappy.securityanalyze.company.application.service;

import org.cwowhappy.securityanalyze.company.application.dto.CompanyDTO;
import org.cwowhappy.securityanalyze.shared.dto.PageQuery;
import org.cwowhappy.securityanalyze.shared.dto.PageResult;

import java.util.Optional;

/**
 * 公司应用服务接口。
 */
public interface CompanyAppService {

    PageResult<CompanyDTO> findByPage(PageQuery pageQuery, String industry, String province, String keyword);

    Optional<CompanyDTO> findByUscCode(String uscCode);

    String createCompany(CompanyDTO dto);
}
