package org.cwowhappy.securityanalyze.stock.infrastructure.persistence.mapper;

import org.cwowhappy.securityanalyze.stock.infrastructure.persistence.entity.StockEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * StockRowMapper 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class StockRowMapperTest {

    private final StockRowMapper mapper = new StockRowMapper();

    @Test
    void shouldMapResultSetToStockEntity() throws Exception {
        // 准备 Mock 数据
        ResultSet rs = mock(ResultSet.class);
        LocalDateTime now = LocalDateTime.of(2026, 5, 10, 12, 0, 0);

        when(rs.getString("id")).thenReturn("stk123");
        when(rs.getString("stock_code")).thenReturn("000001");
        when(rs.getString("name")).thenReturn("平安银行");
        when(rs.getString("market")).thenReturn("主板");
        when(rs.getString("ts_code")).thenReturn("000001.SZ");
        when(rs.getString("full_name")).thenReturn("平安银行股份有限公司");
        when(rs.getString("exchange")).thenReturn("SZ");
        when(rs.getDate("list_date")).thenReturn(Date.valueOf("1991-04-03"));
        when(rs.getString("industry")).thenReturn("银行");
        when(rs.getString("area")).thenReturn("深圳");
        when(rs.getObject("total_shares", Long.class)).thenReturn(19405918198L);
        when(rs.getObject("float_shares", Long.class)).thenReturn(19405562184L);
        when(rs.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(now));
        when(rs.getTimestamp("updated_at")).thenReturn(Timestamp.valueOf(now));

        // 执行映射
        StockEntity entity = mapper.mapRow(rs, 1);

        // 验证所有字段
        assertThat(entity.getId()).isEqualTo("stk123");
        assertThat(entity.getStockCode()).isEqualTo("000001");
        assertThat(entity.getName()).isEqualTo("平安银行");
        assertThat(entity.getMarket()).isEqualTo("主板");
        assertThat(entity.getTsCode()).isEqualTo("000001.SZ");
        assertThat(entity.getFullName()).isEqualTo("平安银行股份有限公司");
        assertThat(entity.getExchange()).isEqualTo("SZ");
        assertThat(entity.getListDate()).isEqualTo(LocalDate.of(1991, 4, 3));
        assertThat(entity.getIndustry()).isEqualTo("银行");
        assertThat(entity.getArea()).isEqualTo("深圳");
        assertThat(entity.getTotalShares()).isEqualTo(19405918198L);
        assertThat(entity.getFloatShares()).isEqualTo(19405562184L);
        assertThat(entity.getCreatedAt()).isEqualTo(now);
        assertThat(entity.getUpdatedAt()).isEqualTo(now);
    }
}
