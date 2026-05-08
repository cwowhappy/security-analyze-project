package com.example.securityanalyze.portfolio.infrastructure;

import com.example.securityanalyze.portfolio.domain.TradeType;
import com.example.securityanalyze.portfolio.domain.TransactionRecord;
import com.example.securityanalyze.portfolio.domain.TransactionRepository;
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
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TransactionRepositoryImpl implements TransactionRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final RowMapper<TransactionRecord> ROW_MAPPER = new RowMapper<>() {
        @Override
        public TransactionRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            TransactionRecord t = new TransactionRecord();
            t.setId(rs.getLong("id"));
            t.setPortfolioId(rs.getLong("portfolio_id"));
            t.setStockCode(rs.getString("stock_code"));
            t.setTradeDate(rs.getDate("trade_date").toLocalDate());
            t.setTradeType(TradeType.valueOf(rs.getString("trade_type")));
            t.setPrice(rs.getBigDecimal("price"));
            t.setQuantity(rs.getBigDecimal("quantity"));
            t.setFee(rs.getBigDecimal("fee"));
            t.setTax(rs.getBigDecimal("tax"));
            t.setAmount(rs.getBigDecimal("amount"));
            t.setRealizedPnl(rs.getBigDecimal("realized_pnl"));
            t.setRemark(rs.getString("remark"));
            t.setIsDeleted(rs.getBoolean("is_deleted"));

            Timestamp deletedAt = rs.getTimestamp("deleted_at");
            if (deletedAt != null) t.setDeletedAt(deletedAt.toLocalDateTime());

            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) t.setCreatedAt(createdAt.toLocalDateTime());

            return t;
        }
    };

    @Override
    public TransactionRecord save(TransactionRecord transaction) {
        String sql = """
                INSERT INTO transaction_record (portfolio_id, stock_code, trade_date, trade_type, price, quantity,
                                                fee, tax, amount, realized_pnl, remark, is_deleted, deleted_at, created_at)
                VALUES (:portfolioId, :stockCode, :tradeDate, :tradeType::trade_type, :price, :quantity,
                        :fee, :tax, :amount, :realizedPnl, :remark, FALSE, NULL, NOW())
                RETURNING id, portfolio_id, stock_code, trade_date, trade_type, price, quantity,
                          fee, tax, amount, realized_pnl, remark, is_deleted, deleted_at, created_at
                """;
        MapSqlParameterSource params = mapParams(transaction);
        return jdbcTemplate.queryForObject(sql, params, ROW_MAPPER);
    }

    @Override
    public Optional<TransactionRecord> findById(Long id) {
        String sql = "SELECT * FROM transaction_record WHERE id = :id AND is_deleted = FALSE";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);
        List<TransactionRecord> results = jdbcTemplate.query(sql, params, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Optional<TransactionRecord> findByIdAndPortfolioId(Long id, Long portfolioId) {
        String sql = "SELECT * FROM transaction_record WHERE id = :id AND portfolio_id = :portfolioId AND is_deleted = FALSE";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);
        params.addValue("portfolioId", portfolioId);
        List<TransactionRecord> results = jdbcTemplate.query(sql, params, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<TransactionRecord> findByPortfolioId(Long portfolioId, String stockCode, TradeType tradeType,
                                                       String startDate, String endDate, int offset, int limit) {
        StringBuilder sql = new StringBuilder("SELECT * FROM transaction_record WHERE portfolio_id = :portfolioId AND is_deleted = FALSE");
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("portfolioId", portfolioId);

        if (stockCode != null && !stockCode.isBlank()) {
            sql.append(" AND stock_code = :stockCode");
            params.addValue("stockCode", stockCode);
        }
        if (tradeType != null) {
            sql.append(" AND trade_type = :tradeType::trade_type");
            params.addValue("tradeType", tradeType.name());
        }
        if (startDate != null && !startDate.isBlank()) {
            sql.append(" AND trade_date >= CAST(:startDate AS DATE)");
            params.addValue("startDate", startDate);
        }
        if (endDate != null && !endDate.isBlank()) {
            sql.append(" AND trade_date <= CAST(:endDate AS DATE)");
            params.addValue("endDate", endDate);
        }
        sql.append(" ORDER BY trade_date DESC, id DESC LIMIT :limit OFFSET :offset");
        params.addValue("limit", limit);
        params.addValue("offset", offset);

        return jdbcTemplate.query(sql.toString(), params, ROW_MAPPER);
    }

    @Override
    public long countByPortfolioId(Long portfolioId, String stockCode, TradeType tradeType,
                                   String startDate, String endDate) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM transaction_record WHERE portfolio_id = :portfolioId AND is_deleted = FALSE");
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("portfolioId", portfolioId);

        if (stockCode != null && !stockCode.isBlank()) {
            sql.append(" AND stock_code = :stockCode");
            params.addValue("stockCode", stockCode);
        }
        if (tradeType != null) {
            sql.append(" AND trade_type = :tradeType::trade_type");
            params.addValue("tradeType", tradeType.name());
        }
        if (startDate != null && !startDate.isBlank()) {
            sql.append(" AND trade_date >= CAST(:startDate AS DATE)");
            params.addValue("startDate", startDate);
        }
        if (endDate != null && !endDate.isBlank()) {
            sql.append(" AND trade_date <= CAST(:endDate AS DATE)");
            params.addValue("endDate", endDate);
        }

        Long count = jdbcTemplate.queryForObject(sql.toString(), params, Long.class);
        return count != null ? count : 0;
    }

    @Override
    public void update(TransactionRecord transaction) {
        String sql = """
                UPDATE transaction_record
                SET stock_code = :stockCode, trade_date = :tradeDate, trade_type = :tradeType::trade_type,
                    price = :price, quantity = :quantity, fee = :fee, tax = :tax,
                    amount = :amount, realized_pnl = :realizedPnl, remark = :remark
                WHERE id = :id AND is_deleted = FALSE
                """;
        MapSqlParameterSource params = mapParams(transaction);
        params.addValue("id", transaction.getId());
        jdbcTemplate.update(sql, params);
    }

    @Override
    public void softDelete(Long id) {
        String sql = "UPDATE transaction_record SET is_deleted = TRUE, deleted_at = NOW() WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);
        jdbcTemplate.update(sql, params);
    }

    @Override
    public List<TransactionRecord> findActiveByPortfolioIdAndStockCode(Long portfolioId, String stockCode) {
        String sql = """
                SELECT * FROM transaction_record
                WHERE portfolio_id = :portfolioId AND stock_code = :stockCode AND is_deleted = FALSE
                ORDER BY trade_date ASC, id ASC
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("portfolioId", portfolioId);
        params.addValue("stockCode", stockCode);
        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    private MapSqlParameterSource mapParams(TransactionRecord t) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("portfolioId", t.getPortfolioId());
        params.addValue("stockCode", t.getStockCode());
        params.addValue("tradeDate", t.getTradeDate());
        params.addValue("tradeType", t.getTradeType().name());
        params.addValue("price", t.getPrice());
        params.addValue("quantity", t.getQuantity());
        params.addValue("fee", t.getFee());
        params.addValue("tax", t.getTax());
        params.addValue("amount", t.getAmount());
        params.addValue("realizedPnl", t.getRealizedPnl());
        params.addValue("remark", t.getRemark());
        return params;
    }
}
