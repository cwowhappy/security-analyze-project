package org.cwowhappy.securityanalyze.stock.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.cwowhappy.securityanalyze.stock.domain.model.Stock;
import org.cwowhappy.securityanalyze.stock.domain.model.StockId;
import org.cwowhappy.securityanalyze.stock.domain.repository.StockRepository;
import org.cwowhappy.securityanalyze.stock.infrastructure.persistence.entity.StockEntity;
import org.cwowhappy.securityanalyze.stock.infrastructure.persistence.mapper.StockRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 股票仓库 JDBC 实现（Adapter）。
 */
@Repository
@RequiredArgsConstructor
public class JdbcStockRepository implements StockRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final StockRowMapper rowMapper;

    @Override
    public Optional<Stock> findById(StockId id) {
        String sql = "SELECT * FROM stock WHERE id = :id";
        List<StockEntity> results = jdbcTemplate.query(sql,
                new MapSqlParameterSource("id", id.getValue()), rowMapper);
        return results.stream().findFirst().map(this::toDomain);
    }

    @Override
    public Optional<Stock> findBySymbol(String symbol) {
        String sql = "SELECT * FROM stock WHERE symbol = :symbol";
        List<StockEntity> results = jdbcTemplate.query(sql,
                new MapSqlParameterSource("symbol", symbol), rowMapper);
        return results.stream().findFirst().map(this::toDomain);
    }

    @Override
    public List<Stock> findAll() {
        String sql = "SELECT * FROM stock ORDER BY symbol";
        return jdbcTemplate.query(sql, rowMapper).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public StockId save(Stock stock) {
        String sql = """
                INSERT INTO stock (id, symbol, name, market, current_price, change_percent, updated_at)
                VALUES (:id, :symbol, :name, :market, :currentPrice, :changePercent, :updatedAt)
                ON CONFLICT (id) DO UPDATE SET
                    symbol = EXCLUDED.symbol,
                    name = EXCLUDED.name,
                    market = EXCLUDED.market,
                    current_price = EXCLUDED.current_price,
                    change_percent = EXCLUDED.change_percent,
                    updated_at = EXCLUDED.updated_at
                """;
        StockEntity entity = toEntity(stock);
        SqlParameterSource params = new BeanPropertySqlParameterSource(entity);
        jdbcTemplate.update(sql, params);
        return stock.getId();
    }

    @Override
    public void deleteById(StockId id) {
        String sql = "DELETE FROM stock WHERE id = :id";
        jdbcTemplate.update(sql, new MapSqlParameterSource("id", id.getValue()));
    }

    private Stock toDomain(StockEntity entity) {
        return Stock.builder()
                .id(StockId.of(entity.getId()))
                .symbol(entity.getSymbol())
                .name(entity.getName())
                .market(entity.getMarket())
                .currentPrice(entity.getCurrentPrice())
                .changePercent(entity.getChangePercent())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private StockEntity toEntity(Stock stock) {
        StockEntity entity = new StockEntity();
        entity.setId(stock.getId().getValue());
        entity.setSymbol(stock.getSymbol());
        entity.setName(stock.getName());
        entity.setMarket(stock.getMarket());
        entity.setCurrentPrice(stock.getCurrentPrice());
        entity.setChangePercent(stock.getChangePercent());
        entity.setUpdatedAt(stock.getUpdatedAt());
        return entity;
    }
}
