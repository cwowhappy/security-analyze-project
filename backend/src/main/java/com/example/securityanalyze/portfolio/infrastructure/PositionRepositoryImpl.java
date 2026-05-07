package com.example.securityanalyze.portfolio.infrastructure;

import com.example.securityanalyze.portfolio.domain.Position;
import com.example.securityanalyze.portfolio.domain.PositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PositionRepositoryImpl implements PositionRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final RowMapper<Position> ROW_MAPPER = new RowMapper<>() {
        @Override
        public Position mapRow(ResultSet rs, int rowNum) throws SQLException {
            Position p = new Position();
            p.setId(rs.getLong("id"));
            p.setPortfolioId(rs.getLong("portfolio_id"));
            p.setStockCode(rs.getString("stock_code"));
            p.setCurrentQuantity(rs.getBigDecimal("current_quantity"));
            p.setTotalCost(rs.getBigDecimal("total_cost"));
            p.setAvgCost(rs.getBigDecimal("avg_cost"));
            p.setRealizedPnl(rs.getBigDecimal("realized_pnl"));

            java.sql.Date firstBuyDate = rs.getDate("first_buy_date");
            if (firstBuyDate != null) p.setFirstBuyDate(firstBuyDate.toLocalDate());

            java.sql.Date lastTradeDate = rs.getDate("last_trade_date");
            if (lastTradeDate != null) p.setLastTradeDate(lastTradeDate.toLocalDate());

            p.setIsDeleted(rs.getBoolean("is_deleted"));

            Timestamp deletedAt = rs.getTimestamp("deleted_at");
            if (deletedAt != null) p.setDeletedAt(deletedAt.toLocalDateTime());

            Timestamp updatedAt = rs.getTimestamp("updated_at");
            if (updatedAt != null) p.setUpdatedAt(updatedAt.toLocalDateTime());

            return p;
        }
    };

    @Override
    public Position save(Position position) {
        String sql = """
                INSERT INTO position (portfolio_id, stock_code, current_quantity, total_cost, avg_cost,
                                      realized_pnl, first_buy_date, last_trade_date, is_deleted, deleted_at, updated_at)
                VALUES (:portfolioId, :stockCode, :currentQuantity, :totalCost, :avgCost,
                        :realizedPnl, :firstBuyDate, :lastTradeDate, FALSE, NULL, NOW())
                RETURNING id, portfolio_id, stock_code, current_quantity, total_cost, avg_cost,
                          realized_pnl, first_buy_date, last_trade_date, is_deleted, deleted_at, updated_at
                """;
        MapSqlParameterSource params = mapParams(position);
        return jdbcTemplate.queryForObject(sql, params, ROW_MAPPER);
    }

    @Override
    public Optional<Position> findById(Long id) {
        String sql = "SELECT * FROM position WHERE id = :id AND is_deleted = FALSE";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);
        List<Position> results = jdbcTemplate.query(sql, params, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Optional<Position> findByPortfolioIdAndStockCode(Long portfolioId, String stockCode) {
        String sql = "SELECT * FROM position WHERE portfolio_id = :portfolioId AND stock_code = :stockCode AND is_deleted = FALSE";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("portfolioId", portfolioId);
        params.addValue("stockCode", stockCode);
        List<Position> results = jdbcTemplate.query(sql, params, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<Position> findByPortfolioId(Long portfolioId) {
        String sql = "SELECT * FROM position WHERE portfolio_id = :portfolioId AND is_deleted = FALSE ORDER BY stock_code";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("portfolioId", portfolioId);
        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    @Override
    public List<Map<String, Object>> findByPortfolioIdWithQuote(Long portfolioId) {
        String sql = """
                SELECT
                    p.stock_code,
                    p.current_quantity,
                    p.total_cost,
                    p.avg_cost,
                    p.realized_pnl,
                    p.first_buy_date,
                    p.last_trade_date,
                    cs.stock_name,
                    cs.market,
                    c.industry,
                    dq.close_price
                FROM position p
                LEFT JOIN company_security cs ON p.stock_code = cs.stock_code
                LEFT JOIN company c ON cs.company_id = c.id
                LEFT JOIN LATERAL (
                    SELECT close_price FROM daily_quote
                    WHERE stock_code = p.stock_code
                    ORDER BY trade_date DESC LIMIT 1
                ) dq ON true
                WHERE p.portfolio_id = :portfolioId AND p.is_deleted = FALSE
                ORDER BY p.stock_code
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("portfolioId", portfolioId);
        return jdbcTemplate.queryForList(sql, params);
    }

    @Override
    public void update(Position position) {
        String sql = """
                UPDATE position
                SET current_quantity = :currentQuantity, total_cost = :totalCost, avg_cost = :avgCost,
                    realized_pnl = :realizedPnl, first_buy_date = :firstBuyDate,
                    last_trade_date = :lastTradeDate, updated_at = NOW()
                WHERE id = :id AND is_deleted = FALSE
                """;
        MapSqlParameterSource params = mapParams(position);
        params.addValue("id", position.getId());
        jdbcTemplate.update(sql, params);
    }

    @Override
    public void softDelete(Long id) {
        String sql = "UPDATE position SET is_deleted = TRUE, deleted_at = NOW() WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);
        jdbcTemplate.update(sql, params);
    }

    @Override
    public void softDeleteByPortfolioIdAndStockCode(Long portfolioId, String stockCode) {
        String sql = "UPDATE position SET is_deleted = TRUE, deleted_at = NOW() WHERE portfolio_id = :portfolioId AND stock_code = :stockCode";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("portfolioId", portfolioId);
        params.addValue("stockCode", stockCode);
        jdbcTemplate.update(sql, params);
    }

    private MapSqlParameterSource mapParams(Position p) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("portfolioId", p.getPortfolioId());
        params.addValue("stockCode", p.getStockCode());
        params.addValue("currentQuantity", p.getCurrentQuantity());
        params.addValue("totalCost", p.getTotalCost());
        params.addValue("avgCost", p.getAvgCost());
        params.addValue("realizedPnl", p.getRealizedPnl());
        params.addValue("firstBuyDate", p.getFirstBuyDate());
        params.addValue("lastTradeDate", p.getLastTradeDate());
        return params;
    }
}
