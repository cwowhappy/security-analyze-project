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
        entity.setSymbol(rs.getString("symbol"));
        entity.setName(rs.getString("name"));
        entity.setMarket(rs.getString("market"));
        entity.setCurrentPrice(rs.getBigDecimal("current_price"));
        entity.setChangePercent(rs.getBigDecimal("change_percent"));
        entity.setUpdatedAt(rs.getTimestamp("updated_at") != null
                ? rs.getTimestamp("updated_at").toLocalDateTime()
                : null);
        return entity;
    }
}
