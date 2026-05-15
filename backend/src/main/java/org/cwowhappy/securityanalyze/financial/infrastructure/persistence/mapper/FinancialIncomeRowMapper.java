package org.cwowhappy.securityanalyze.financial.infrastructure.persistence.mapper;

import org.cwowhappy.securityanalyze.financial.infrastructure.persistence.entity.FinancialIncomeEntity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * FinancialIncome JDBC RowMapper。
 */
@Component
public class FinancialIncomeRowMapper implements RowMapper<FinancialIncomeEntity> {

    @Override
    public FinancialIncomeEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
        FinancialIncomeEntity entity = new FinancialIncomeEntity();
        entity.setId(rs.getString("id"));
        entity.setStockCode(rs.getString("stock_code"));
        java.sql.Date reportDate = rs.getDate("report_date");
        entity.setReportDate(reportDate != null ? reportDate.toLocalDate() : null);
        entity.setReportType(rs.getString("report_type"));
        entity.setBasicEps(rs.getBigDecimal("basic_eps"));
        entity.setDilutedEps(rs.getBigDecimal("diluted_eps"));
        entity.setTotalRevenue(rs.getBigDecimal("total_revenue"));
        entity.setRevenue(rs.getBigDecimal("revenue"));
        entity.setOperatingCost(rs.getBigDecimal("operating_cost"));
        entity.setGrossProfit(rs.getBigDecimal("gross_profit"));
        entity.setSellingExpense(rs.getBigDecimal("selling_expense"));
        entity.setAdminExpense(rs.getBigDecimal("admin_expense"));
        entity.setRdExpense(rs.getBigDecimal("rd_expense"));
        entity.setFinancialExpense(rs.getBigDecimal("financial_expense"));
        entity.setOperatingProfit(rs.getBigDecimal("operating_profit"));
        entity.setTotalProfit(rs.getBigDecimal("total_profit"));
        entity.setNetProfit(rs.getBigDecimal("net_profit"));
        entity.setNpParentCompany(rs.getBigDecimal("np_parent_company"));
        entity.setNpExclNonrecurring(rs.getBigDecimal("np_excl_nonrecurring"));
        entity.setCreatedAt(rs.getTimestamp("created_at") != null
                ? rs.getTimestamp("created_at").toLocalDateTime()
                : null);
        entity.setUpdatedAt(rs.getTimestamp("updated_at") != null
                ? rs.getTimestamp("updated_at").toLocalDateTime()
                : null);
        return entity;
    }
}
