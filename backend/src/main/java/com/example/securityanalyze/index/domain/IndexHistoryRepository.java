package com.example.securityanalyze.index.domain;

import java.time.LocalDate;
import java.util.List;

public interface IndexHistoryRepository {

    List<IndexHistory> findByIndexCodeAndGranularity(String indexCode, String granularity,
                                                       LocalDate startDate, LocalDate endDate);

    List<IndexHistory> findByIndexCodeAndGranularity(String indexCode, String granularity,
                                                       int offset, int limit);

    long countByIndexCodeAndGranularity(String indexCode, String granularity);

    void saveAll(List<IndexHistory> histories);
}
