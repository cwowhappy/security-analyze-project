package org.cwowhappy.securityanalyze.financial.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.cwowhappy.securityanalyze.financial.domain.model.FinancialIncome;
import org.cwowhappy.securityanalyze.financial.domain.repository.FinancialIncomeRepository;
import org.cwowhappy.securityanalyze.financial.infrastructure.persistence.entity.FinancialIncomeEntity;
import org.cwowhappy.securityanalyze.financial.infrastructure.persistence.mapper.FinancialIncomeRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 利润表 JDBC 仓库实现。
 */
@Repository
@RequiredArgsConstructor
public class JdbcFinancialIncomeRepository implements FinancialIncomeRepository {

    private final JdbcTemplate jdbcTemplate;
    private final FinancialIncomeRowMapper rowMapper;

    @Override
    @Transactional
    public void save(FinancialIncome income) {
        String id = income.getId() != null ? income.getId() : UUID.randomUUID().toString().replace("-", "");
        String sql = """
            INSERT INTO tb_financial_income (
                id, stock_code, report_date, report_type, basic_eps, diluted_eps,
                total_revenue, revenue, operating_cost, gross_profit,
                selling_expense, admin_expense, rd_expense, financial_expense,
                operating_profit, total_profit, net_profit, np_parent_company,
                np_excl_nonrecurring, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
            ON CONFLICT (stock_code, report_date, report_type) DO UPDATE SET
                basic_eps = EXCLUDED.basic_eps,
                diluted_eps = EXCLUDED.diluted_eps,
                total_revenue = EXCLUDED.total_revenue,
                revenue = EXCLUDED.revenue,
                operating_cost = EXCLUDED.operating_cost,
                gross_profit = EXCLUDED.gross_profit,
                selling_expense = EXCLUDED.selling_expense,
                admin_expense = EXCLUDED.admin_expense,
                rd_expense = EXCLUDED.rd_expense,
                financial_expense = EXCLUDED.financial_expense,
                operating_profit = EXCLUDED.operating_profit,
                total_profit = EXCLUDED.total_profit,
                net_profit = EXCLUDED.net_profit,
                np_parent_company = EXCLUDED.np_parent_company,
                np_excl_nonrecurring = EXCLUDED.np_excl_nonrecurring,
                updated_at = NOW()
            """;
        jdbcTemplate.update(sql,
            id, income.getStockCode(), income.getReportDate(), income.getReportType(),
            income.getBasicEps(), income.getDilutedEps(), income.getTotalRevenue(), income.getRevenue(),
            income.getOperatingCost(), income.getGrossProfit(), income.getSellingExpense(),
            income.getAdminExpense(), income.getRdExpense(), income.getFinancialExpense(),
            income.getOperatingProfit(), income.getTotalProfit(), income.getNetProfit(),
            income.getNpParentCompany(), income.getNpExclNonrecurring()
        );
    }

    @Override
    @Transactional
    public void saveAll(List<FinancialIncome> incomes) {
        for (FinancialIncome income : incomes) {
            save(income);
        }
    }

    @Override
    public List<FinancialIncome> findByStockCode(String stockCode) {
        return findByStockCode(stockCode, null, 20);
    }

    @Override
    public List<FinancialIncome> findByStockCode(String stockCode, String reportType) {
        return findByStockCode(stockCode, reportType, 20);
    }

    @Override
    public List<FinancialIncome> findByStockCode(String stockCode, String reportType, int limit) {
        String sql;
        List<FinancialIncomeEntity> entities;
        if (reportType != null) {
            sql = "SELECT * FROM tb_financial_income WHERE stock_code = ? AND report_type = ? ORDER BY report_date DESC LIMIT ?";
            entities = jdbcTemplate.query(sql, rowMapper, stockCode, reportType, limit);
        } else {
            sql = "SELECT * FROM tb_financial_income WHERE stock_code = ? ORDER BY report_date DESC LIMIT ?";
            entities = jdbcTemplate.query(sql, rowMapper, stockCode, limit);
        }
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<FinancialIncome> findLatest(String stockCode, String reportType) {
        String sql = "SELECT * FROM tb_financial_income WHERE stock_code = ? AND report_type = ? ORDER BY report_date DESC LIMIT 1";
        List<FinancialIncomeEntity> entities = jdbcTemplate.query(sql, rowMapper, stockCode, reportType);
        return entities.stream().findFirst().map(this::toDomain);
    }

    @Override
    public Optional<FinancialIncome> findByStockCodeAndReportDate(String stockCode, LocalDate reportDate, String reportType) {
        String sql = "SELECT * FROM tb_financial_income WHERE stock_code = ? AND report_date = ? AND report_type = ?";
        List<FinancialIncomeEntity> entities = jdbcTemplate.query(sql, rowMapper, stockCode, reportDate, reportType);
        return entities.stream().findFirst().map(this::toDomain);
    }

    private FinancialIncome toDomain(FinancialIncomeEntity entity) {
        return FinancialIncome.builder()
            .id(entity.getId())
            .stockCode(entity.getStockCode())
            .reportDate(entity.getReportDate())
            .reportType(entity.getReportType())
            .basicEps(entity.getBasicEps())
            .dilutedEps(entity.getDilutedEps())
            .totalRevenue(entity.getTotalRevenue())
            .revenue(entity.getRevenue())
            .operatingCost(entity.getOperatingCost())
            .grossProfit(entity.getGrossProfit())
            .sellingExpense(entity.getSellingExpense())
            .adminExpense(entity.getAdminExpense())
            .rdExpense(entity.getRdExpense())
            .financialExpense(entity.getFinancialExpense())
            .operatingProfit(entity.getOperatingProfit())
            .totalProfit(entity.getTotalProfit())
            .netProfit(entity.getNetProfit())
            .npParentCompany(entity.getNpParentCompany())
            .npExclNonrecurring(entity.getNpExclNonrecurring())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}
