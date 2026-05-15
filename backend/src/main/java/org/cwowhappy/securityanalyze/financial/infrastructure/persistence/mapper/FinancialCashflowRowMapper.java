package org.cwowhappy.securityanalyze.financial.infrastructure.persistence.mapper;

import org.cwowhappy.securityanalyze.financial.infrastructure.persistence.entity.FinancialCashflowEntity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * FinancialCashflow JDBC RowMapper。
 */
@Component
public class FinancialCashflowRowMapper implements RowMapper<FinancialCashflowEntity> {

    @Override
    public FinancialCashflowEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
        FinancialCashflowEntity entity = new FinancialCashflowEntity();
        entity.setId(rs.getString("id"));
        entity.setStockCode(rs.getString("stock_code"));
        java.sql.Date reportDate = rs.getDate("report_date");
        entity.setReportDate(reportDate != null ? reportDate.toLocalDate() : null);
        entity.setReportType(rs.getString("report_type"));
        entity.setCfOperating(rs.getBigDecimal("cf_operating"));
        entity.setCfInvesting(rs.getBigDecimal("cf_investing"));
        entity.setCfFinancing(rs.getBigDecimal("cf_financing"));
        entity.setNetCashFlow(rs.getBigDecimal("net_cash_flow"));
        entity.setFreeCashFlow(rs.getBigDecimal("free_cash_flow"));
        entity.setCapex(rs.getBigDecimal("capex"));
        entity.setCashReceivedOperating(rs.getBigDecimal("cash_received_operating"));
        entity.setTaxPaid(rs.getBigDecimal("tax_paid"));
        entity.setCreatedAt(rs.getTimestamp("created_at") != null
                ? rs.getTimestamp("created_at").toLocalDateTime()
                : null);
        entity.setUpdatedAt(rs.getTimestamp("updated_at") != null
                ? rs.getTimestamp("updated_at").toLocalDateTime()
                : null);
        return entity;
    }
}
