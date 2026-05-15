package org.cwowhappy.securityanalyze.financial.infrastructure.persistence.mapper;

import org.cwowhappy.securityanalyze.financial.infrastructure.persistence.entity.FinancialIndicatorEntity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * FinancialIndicator JDBC RowMapper。
 */
@Component
public class FinancialIndicatorRowMapper implements RowMapper<FinancialIndicatorEntity> {

    @Override
    public FinancialIndicatorEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
        FinancialIndicatorEntity entity = new FinancialIndicatorEntity();
        entity.setId(rs.getString("id"));
        entity.setStockCode(rs.getString("stock_code"));
        java.sql.Date reportDate = rs.getDate("report_date");
        entity.setReportDate(reportDate != null ? reportDate.toLocalDate() : null);
        entity.setReportType(rs.getString("report_type"));

        // 盈利能力
        entity.setRoe(rs.getBigDecimal("roe"));
        entity.setRoa(rs.getBigDecimal("roa"));
        entity.setRoic(rs.getBigDecimal("roic"));
        entity.setGrossMargin(rs.getBigDecimal("gross_margin"));
        entity.setNetMargin(rs.getBigDecimal("net_margin"));
        entity.setNetMarginExcl(rs.getBigDecimal("net_margin_excl"));

        // 偿债能力
        entity.setDebtRatio(rs.getBigDecimal("debt_ratio"));
        entity.setCurrentRatio(rs.getBigDecimal("current_ratio"));
        entity.setQuickRatio(rs.getBigDecimal("quick_ratio"));
        entity.setNetDebtRatio(rs.getBigDecimal("net_debt_ratio"));
        entity.setEquityRatio(rs.getBigDecimal("equity_ratio"));

        // 运营效率
        entity.setDso(rs.getBigDecimal("dso"));
        entity.setDio(rs.getBigDecimal("dio"));
        entity.setDpo(rs.getBigDecimal("dpo"));
        entity.setCcc(rs.getBigDecimal("ccc"));
        entity.setAssetTurnover(rs.getBigDecimal("asset_turnover"));
        entity.setFixedAssetTurnover(rs.getBigDecimal("fixed_asset_turnover"));

        // 成长性
        entity.setRevenueGrowth(rs.getBigDecimal("revenue_growth"));
        entity.setNpParentGrowth(rs.getBigDecimal("np_parent_growth"));
        entity.setNpExclGrowth(rs.getBigDecimal("np_excl_growth"));
        entity.setCfoGrowth(rs.getBigDecimal("cfo_growth"));
        entity.setEquityGrowth(rs.getBigDecimal("equity_growth"));
        entity.setAssetGrowth(rs.getBigDecimal("asset_growth"));

        // 估值
        entity.setPe(rs.getBigDecimal("pe"));
        entity.setPb(rs.getBigDecimal("pb"));
        entity.setPs(rs.getBigDecimal("ps"));
        entity.setPeg(rs.getBigDecimal("peg"));
        entity.setEvEbitda(rs.getBigDecimal("ev_ebitda"));
        entity.setDividendYield(rs.getBigDecimal("dividend_yield"));
        entity.setMarketCap(rs.getBigDecimal("market_cap"));

        // 现金流质量
        entity.setCfoToNp(rs.getBigDecimal("cfo_to_np"));

        // 元数据
        entity.setDataSource(rs.getString("data_source"));
        entity.setCreatedAt(rs.getTimestamp("created_at") != null
                ? rs.getTimestamp("created_at").toLocalDateTime()
                : null);
        entity.setUpdatedAt(rs.getTimestamp("updated_at") != null
                ? rs.getTimestamp("updated_at").toLocalDateTime()
                : null);
        return entity;
    }
}
