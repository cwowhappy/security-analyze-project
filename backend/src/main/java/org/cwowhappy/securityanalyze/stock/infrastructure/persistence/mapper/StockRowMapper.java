package org.cwowhappy.securityanalyze.stock.infrastructure.persistence.mapper;

import org.cwowhappy.securityanalyze.stock.infrastructure.persistence.entity.StockEntity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Stock JDBC RowMapper。
 */
@Component
public class StockRowMapper implements RowMapper<StockEntity> {

    @Override
    public StockEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
        StockEntity entity = new StockEntity();
        entity.setId(rs.getString("id"));
        entity.setStockCode(rs.getString("stock_code"));
        entity.setName(rs.getString("name"));
        entity.setMarket(rs.getString("market"));
        entity.setTsCode(rs.getString("ts_code"));
        entity.setFullName(rs.getString("full_name"));
        entity.setExchange(rs.getString("exchange"));
        java.sql.Date listDate = rs.getDate("list_date");
        entity.setListDate(listDate != null ? listDate.toLocalDate() : null);
        entity.setIndustry(rs.getString("industry"));
        entity.setArea(rs.getString("area"));
        entity.setTotalShares(rs.getObject("total_shares", Long.class));
        entity.setFloatShares(rs.getObject("float_shares", Long.class));
        entity.setCreatedAt(rs.getTimestamp("created_at") != null
                ? rs.getTimestamp("created_at").toLocalDateTime()
                : null);
        entity.setUpdatedAt(rs.getTimestamp("updated_at") != null
                ? rs.getTimestamp("updated_at").toLocalDateTime()
                : null);
        return entity;
    }
}
