package org.cwowhappy.securityanalyze.financial.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.cwowhappy.securityanalyze.financial.domain.model.FinancialBalance;
import org.cwowhappy.securityanalyze.financial.domain.repository.FinancialBalanceRepository;
import org.cwowhappy.securityanalyze.financial.infrastructure.persistence.entity.FinancialBalanceEntity;
import org.cwowhappy.securityanalyze.financial.infrastructure.persistence.mapper.FinancialBalanceRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 资产负债表 JDBC 仓库实现。
 */
@Repository
@RequiredArgsConstructor
public class JdbcFinancialBalanceRepository implements FinancialBalanceRepository {

    private final JdbcTemplate jdbcTemplate;
    private final FinancialBalanceRowMapper rowMapper;

    @Override
    @Transactional
    public void save(FinancialBalance balance) {
        String id = balance.getId() != null ? balance.getId() : UUID.randomUUID().toString().replace("-", "");
        String sql = """
            INSERT INTO tb_financial_balance (
                id, stock_code, report_date, report_type, total_assets, total_liabilities,
                total_equity, equity_parent_company, current_assets, non_current_assets,
                cash_equivalents, accounts_receivable, inventories, current_liabilities,
                non_current_liabilities, accounts_payable, short_term_borrowings,
                long_term_borrowings, goodwill, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
            ON CONFLICT (stock_code, report_date, report_type) DO UPDATE SET
                total_assets = EXCLUDED.total_assets,
                total_liabilities = EXCLUDED.total_liabilities,
                total_equity = EXCLUDED.total_equity,
                equity_parent_company = EXCLUDED.equity_parent_company,
                current_assets = EXCLUDED.current_assets,
                non_current_assets = EXCLUDED.non_current_assets,
                cash_equivalents = EXCLUDED.cash_equivalents,
                accounts_receivable = EXCLUDED.accounts_receivable,
                inventories = EXCLUDED.inventories,
                current_liabilities = EXCLUDED.current_liabilities,
                non_current_liabilities = EXCLUDED.non_current_liabilities,
                accounts_payable = EXCLUDED.accounts_payable,
                short_term_borrowings = EXCLUDED.short_term_borrowings,
                long_term_borrowings = EXCLUDED.long_term_borrowings,
                goodwill = EXCLUDED.goodwill,
                updated_at = NOW()
            """;
        jdbcTemplate.update(sql,
            id, balance.getStockCode(), balance.getReportDate(), balance.getReportType(),
            balance.getTotalAssets(), balance.getTotalLiabilities(), balance.getTotalEquity(),
            balance.getEquityParentCompany(), balance.getCurrentAssets(), balance.getNonCurrentAssets(),
            balance.getCashEquivalents(), balance.getAccountsReceivable(), balance.getInventories(),
            balance.getCurrentLiabilities(), balance.getNonCurrentLiabilities(),
            balance.getAccountsPayable(), balance.getShortTermBorrowings(),
            balance.getLongTermBorrowings(), balance.getGoodwill()
        );
    }

    @Override
    @Transactional
    public void saveAll(List<FinancialBalance> balances) {
        for (FinancialBalance balance : balances) {
            save(balance);
        }
    }

    @Override
    public List<FinancialBalance> findByStockCode(String stockCode) {
        return findByStockCode(stockCode, null, 20);
    }

    @Override
    public List<FinancialBalance> findByStockCode(String stockCode, String reportType) {
        return findByStockCode(stockCode, reportType, 20);
    }

    @Override
    public List<FinancialBalance> findByStockCode(String stockCode, String reportType, int limit) {
        String sql;
        List<FinancialBalanceEntity> entities;
        if (reportType != null) {
            sql = "SELECT * FROM tb_financial_balance WHERE stock_code = ? AND report_type = ? ORDER BY report_date DESC LIMIT ?";
            entities = jdbcTemplate.query(sql, rowMapper, stockCode, reportType, limit);
        } else {
            sql = "SELECT * FROM tb_financial_balance WHERE stock_code = ? ORDER BY report_date DESC LIMIT ?";
            entities = jdbcTemplate.query(sql, rowMapper, stockCode, limit);
        }
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<FinancialBalance> findLatest(String stockCode, String reportType) {
        String sql = "SELECT * FROM tb_financial_balance WHERE stock_code = ? AND report_type = ? ORDER BY report_date DESC LIMIT 1";
        List<FinancialBalanceEntity> entities = jdbcTemplate.query(sql, rowMapper, stockCode, reportType);
        return entities.stream().findFirst().map(this::toDomain);
    }

    @Override
    public Optional<FinancialBalance> findByStockCodeAndReportDate(String stockCode, LocalDate reportDate, String reportType) {
        String sql = "SELECT * FROM tb_financial_balance WHERE stock_code = ? AND report_date = ? AND report_type = ?";
        List<FinancialBalanceEntity> entities = jdbcTemplate.query(sql, rowMapper, stockCode, reportDate, reportType);
        return entities.stream().findFirst().map(this::toDomain);
    }

    private FinancialBalance toDomain(FinancialBalanceEntity entity) {
        return FinancialBalance.builder()
            .id(entity.getId())
            .stockCode(entity.getStockCode())
            .reportDate(entity.getReportDate())
            .reportType(entity.getReportType())
            .totalAssets(entity.getTotalAssets())
            .totalLiabilities(entity.getTotalLiabilities())
            .totalEquity(entity.getTotalEquity())
            .equityParentCompany(entity.getEquityParentCompany())
            .currentAssets(entity.getCurrentAssets())
            .nonCurrentAssets(entity.getNonCurrentAssets())
            .cashEquivalents(entity.getCashEquivalents())
            .accountsReceivable(entity.getAccountsReceivable())
            .inventories(entity.getInventories())
            .currentLiabilities(entity.getCurrentLiabilities())
            .nonCurrentLiabilities(entity.getNonCurrentLiabilities())
            .accountsPayable(entity.getAccountsPayable())
            .shortTermBorrowings(entity.getShortTermBorrowings())
            .longTermBorrowings(entity.getLongTermBorrowings())
            .goodwill(entity.getGoodwill())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}
