package com.example.securityanalyze.finance.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FinancialReportRepository {

    List<FinancialReport> findByStockCode(String stockCode);

    List<FinancialReport> findByStockCodeAndYear(String stockCode, int year);

    List<FinancialReport> findByStockCodeAndDateRange(String stockCode, java.time.LocalDate startDate, java.time.LocalDate endDate);

    Optional<FinancialReport> findById(Long id);

    Optional<FinancialReport> findByStockCodeAndReportDate(String stockCode, LocalDate reportDate);

    void save(FinancialReport report);

    void saveAll(List<FinancialReport> reports);

    boolean existsByStockCodeAndReportDate(String stockCode, LocalDate reportDate);
}
