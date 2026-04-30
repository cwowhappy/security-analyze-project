package com.example.securityanalyze.company.domain;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository {

    List<Company> findByKeyword(String keyword, int offset, int limit);

    long countByKeyword(String keyword);

    Optional<Company> findByStockCode(String stockCode);
}
