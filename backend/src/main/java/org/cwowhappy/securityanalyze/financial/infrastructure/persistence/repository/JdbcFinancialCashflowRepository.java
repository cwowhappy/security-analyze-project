package org.cwowhappy.securityanalyze.financial.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.cwowhappy.securityanalyze.financial.domain.model.FinancialCashflow;
import org.cwowhappy.securityanalyze.financial.domain.repository.FinancialCashflowRepository;
import org.cwowhappy.securityanalyze.financial.infrastructure.persistence.entity.FinancialCashflowEntity;
import org.cwowhappy.securityanalyze.financial.infrastructure.persistence.mapper.FinancialCashflowRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 现金流量表 JDBC 仓库实现。
 */
@Repository
@RequiredArgsConstructor
public class JdbcFinancialCashflowRepository implements FinancialCashflowRepository {

    private final JdbcTemplate jdbcTemplate;
    private final FinancialCashflowRowMapper rowMapper;

    @Override
    @Transactional
    public void save(FinancialCashflow cashflow) {
        String id = cashflow.getId() != null ? cashflow.getId() : UUID.randomUUID().toString().replace("-", "");
        String sql = """
            INSERT INTO tb_financial_cashflow (
                id, stock_code, report_date, report_type, cf_operating, cf_investing,
                cf_financing, net_cash_flow, free_cash_flow, capex,
                cash_received_operating, tax_paid, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
            ON CONFLICT (stock_code, report_date, report_type) DO UPDATE SET
                cf_operating = EXCLUDED.cf_operating,
                cf_investing = EXCLUDED.cf_investing,
                cf_financing = EXCLUDED.cf_financing,
                net_cash_flow = EXCLUDED.net_cash_flow,
                free_cash_flow = EXCLUDED.free_cash_flow,
                capex = EXCLUDED.capex,
                cash_received_operating = EXCLUDED.cash_received_operating,
                tax_paid = EXCLUDED.tax_paid,
                updated_at = NOW()
            """;
        jdbcTemplate.update(sql,
            id, cashflow.getStockCode(), cashflow.getReportDate(), cashflow.getReportType(),
            cashflow.getCfOperating(), cashflow.getCfInvesting(), cashflow.getCfFinancing(),
            cashflow.getNetCashFlow(), cashflow.getFreeCashFlow(), cashflow.getCapex(),
            cashflow.getCashReceivedOperating(), cashflow.getTaxPaid()
        );
    }

    @Override
    @Transactional
    public void saveAll(List<FinancialCashflow> cashflows) {
        for (FinancialCashflow cashflow : cashflows) {
            save(cashflow);
        }
    }

    @Override
    public List<FinancialCashflow> findByStockCode(String stockCode) {
        return findByStockCode(stockCode, null, 20);
    }

    @Override
    public List<FinancialCashflow> findByStockCode(String stockCode, String reportType) {
        return findByStockCode(stockCode, reportType, 20);
    }

    @Override
    public List<FinancialCashflow> findByStockCode(String stockCode, String reportType, int limit) {
        String sql;
        List<FinancialCashflowEntity> entities;
        if (reportType != null) {
            sql = "SELECT * FROM tb_financial_cashflow WHERE stock_code = ? AND report_type = ? ORDER BY report_date DESC LIMIT ?";
            entities = jdbcTemplate.query(sql, rowMapper, stockCode, reportType, limit);
        } else {
            sql = "SELECT * FROM tb_financial_cashflow WHERE stock_code = ? ORDER BY report_date DESC LIMIT ?";
            entities = jdbcTemplate.query(sql, rowMapper, stockCode, limit);
        }
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<FinancialCashflow> findLatest(String stockCode, String reportType) {
        String sql = "SELECT * FROM tb_financial_cashflow WHERE stock_code = ? AND report_type = ? ORDER BY report_date DESC LIMIT 1";
        List<FinancialCashflowEntity> entities = jdbcTemplate.query(sql, rowMapper, stockCode, reportType);
        return entities.stream().findFirst().map(this::toDomain);
    }

    @Override
    public Optional<FinancialCashflow> findByStockCodeAndReportDate(String stockCode, LocalDate reportDate, String reportType) {
        String sql = "SELECT * FROM tb_financial_cashflow WHERE stock_code = ? AND report_date = ? AND report_type = ?";
        List<FinancialCashflowEntity> entities = jdbcTemplate.query(sql, rowMapper, stockCode, reportDate, reportType);
        return entities.stream().findFirst().map(this::toDomain);
    }

    private FinancialCashflow toDomain(FinancialCashflowEntity entity) {
        return FinancialCashflow.builder()
            .id(entity.getId())
            .stockCode(entity.getStockCode())
            .reportDate(entity.getReportDate())
            .reportType(entity.getReportType())
            .cfOperating(entity.getCfOperating())
            .cfInvesting(entity.getCfInvesting())
            .cfFinancing(entity.getCfFinancing())
            .netCashFlow(entity.getNetCashFlow())
            .freeCashFlow(entity.getFreeCashFlow())
            .capex(entity.getCapex())
            .cashReceivedOperating(entity.getCashReceivedOperating())
            .taxPaid(entity.getTaxPaid())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}
