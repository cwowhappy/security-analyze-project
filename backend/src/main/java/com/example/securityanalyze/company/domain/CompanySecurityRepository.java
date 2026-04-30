package com.example.securityanalyze.company.domain;

import java.util.List;
import java.util.Optional;

public interface CompanySecurityRepository {

    List<CompanySecurity> findByCompanyId(Long companyId);

    Optional<CompanySecurity> findByStockCode(String stockCode);

    List<CompanySecurity> findByKeyword(String keyword, int offset, int limit);

    long countByKeyword(String keyword);
}
