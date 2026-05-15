package org.cwowhappy.securityanalyze.financial.infrastructure.persistence.mapper;

import org.cwowhappy.securityanalyze.financial.infrastructure.persistence.entity.FinancialBalanceEntity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * FinancialBalance JDBC RowMapper。
 */
@Component
public class FinancialBalanceRowMapper implements RowMapper<FinancialBalanceEntity> {

    @Override
    public FinancialBalanceEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
        FinancialBalanceEntity entity = new FinancialBalanceEntity();
        entity.setId(rs.getString("id"));
        entity.setStockCode(rs.getString("stock_code"));
        java.sql.Date reportDate = rs.getDate("report_date");
        entity.setReportDate(reportDate != null ? reportDate.toLocalDate() : null);
        entity.setReportType(rs.getString("report_type"));
        entity.setTotalAssets(rs.getBigDecimal("total_assets"));
        entity.setTotalLiabilities(rs.getBigDecimal("total_liabilities"));
        entity.setTotalEquity(rs.getBigDecimal("total_equity"));
        entity.setEquityParentCompany(rs.getBigDecimal("equity_parent_company"));
        entity.setCurrentAssets(rs.getBigDecimal("current_assets"));
        entity.setNonCurrentAssets(rs.getBigDecimal("non_current_assets"));
        entity.setCashEquivalents(rs.getBigDecimal("cash_equivalents"));
        entity.setAccountsReceivable(rs.getBigDecimal("accounts_receivable"));
        entity.setInventories(rs.getBigDecimal("inventories"));
        entity.setCurrentLiabilities(rs.getBigDecimal("current_liabilities"));
        entity.setNonCurrentLiabilities(rs.getBigDecimal("non_current_liabilities"));
        entity.setAccountsPayable(rs.getBigDecimal("accounts_payable"));
        entity.setShortTermBorrowings(rs.getBigDecimal("short_term_borrowings"));
        entity.setLongTermBorrowings(rs.getBigDecimal("long_term_borrowings"));
        entity.setGoodwill(rs.getBigDecimal("goodwill"));
        entity.setCreatedAt(rs.getTimestamp("created_at") != null
                ? rs.getTimestamp("created_at").toLocalDateTime()
                : null);
        entity.setUpdatedAt(rs.getTimestamp("updated_at") != null
                ? rs.getTimestamp("updated_at").toLocalDateTime()
                : null);
        return entity;
    }
}
