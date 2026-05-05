package com.example.securityanalyze.index.infrastructure;

import com.example.securityanalyze.index.domain.IndexHistory;
import com.example.securityanalyze.index.domain.IndexHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class IndexHistoryRepositoryImpl implements IndexHistoryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final String SELECT_SQL = """
            SELECT id, index_code, trade_date, granularity,
                   open_price, high_price, low_price, close_price,
                   volume, amount, amplitude, change_pct, change_amount, turnover_rate,
                   created_at, updated_at
            FROM index_history
            """;

    private static final RowMapper<IndexHistory> ROW_MAPPER = (rs, rowNum) -> {
        IndexHistory h = new IndexHistory();
        h.setId(rs.getLong("id"));
        h.setIndexCode(rs.getString("index_code"));

        java.sql.Date tradeDate = rs.getDate("trade_date");
        if (tradeDate != null) {
            h.setTradeDate(tradeDate.toLocalDate());
        }

        h.setGranularity(rs.getString("granularity"));
        h.setOpenPrice(rs.getBigDecimal("open_price"));
        h.setHighPrice(rs.getBigDecimal("high_price"));
        h.setLowPrice(rs.getBigDecimal("low_price"));
        h.setClosePrice(rs.getBigDecimal("close_price"));

        long volume = rs.getLong("volume");
        if (!rs.wasNull()) {
            h.setVolume(volume);
        }

        h.setAmount(rs.getBigDecimal("amount"));
        h.setAmplitude(rs.getBigDecimal("amplitude"));
        h.setChangePct(rs.getBigDecimal("change_pct"));
        h.setChangeAmount(rs.getBigDecimal("change_amount"));
        h.setTurnoverRate(rs.getBigDecimal("turnover_rate"));

        java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            h.setCreatedAt(createdAt.toLocalDateTime());
        }

        java.sql.Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            h.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return h;
    };

    @Override
    public List<IndexHistory> findByIndexCodeAndGranularity(String indexCode, String granularity,
                                                             LocalDate startDate, LocalDate endDate) {
        log.debug("查询指数历史行情, indexCode={}, granularity={}, startDate={}, endDate={}",
                indexCode, granularity, startDate, endDate);

        String sql = SELECT_SQL + " WHERE index_code = :indexCode AND granularity = :granularity";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("indexCode", indexCode);
        params.addValue("granularity", granularity);

        if (startDate != null) {
            sql += " AND trade_date >= :startDate";
            params.addValue("startDate", startDate);
        }
        if (endDate != null) {
            sql += " AND trade_date <= :endDate";
            params.addValue("endDate", endDate);
        }

        sql += " ORDER BY trade_date ASC";

        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    @Override
    public List<IndexHistory> findByIndexCodeAndGranularity(String indexCode, String granularity,
                                                             int offset, int limit) {
        log.debug("分页查询指数历史行情, indexCode={}, granularity={}, offset={}, limit={}",
                indexCode, granularity, offset, limit);

        String sql = SELECT_SQL + " WHERE index_code = :indexCode AND granularity = :granularity";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("indexCode", indexCode);
        params.addValue("granularity", granularity);
        params.addValue("offset", offset);
        params.addValue("limit", limit);

        sql += " ORDER BY trade_date DESC LIMIT :limit OFFSET :offset";

        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    @Override
    public long countByIndexCodeAndGranularity(String indexCode, String granularity) {
        log.debug("统计指数历史行情数量, indexCode={}, granularity={}", indexCode, granularity);

        String sql = "SELECT COUNT(*) FROM index_history WHERE index_code = :indexCode AND granularity = :granularity";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("indexCode", indexCode);
        params.addValue("granularity", granularity);

        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public void saveAll(List<IndexHistory> histories) {
        // 由采集模块通过 Python 直接写入，Java 层暂不实现批量写入
        throw new UnsupportedOperationException("批量写入由采集模块负责");
    }
}
