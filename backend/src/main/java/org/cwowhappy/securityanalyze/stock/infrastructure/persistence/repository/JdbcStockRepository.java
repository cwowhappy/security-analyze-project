package org.cwowhappy.securityanalyze.stock.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.cwowhappy.securityanalyze.shared.dto.PageQuery;
import org.cwowhappy.securityanalyze.shared.dto.PageResult;
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
        String sql = "SELECT * FROM tb_stock_basic WHERE id = :id";
        List<StockEntity> results = jdbcTemplate.query(sql,
                new MapSqlParameterSource("id", id.getValue()), rowMapper);
        return results.stream().findFirst().map(this::toDomain);
    }

    @Override
    public Optional<Stock> findByStockCode(String stockCode) {
        String sql = "SELECT * FROM tb_stock_basic WHERE stock_code = :stockCode";
        List<StockEntity> results = jdbcTemplate.query(sql,
                new MapSqlParameterSource("stockCode", stockCode), rowMapper);
        return results.stream().findFirst().map(this::toDomain);
    }

    @Override
    public List<Stock> findAll() {
        String sql = "SELECT * FROM tb_stock_basic ORDER BY stock_code";
        return jdbcTemplate.query(sql, rowMapper).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public PageResult<Stock> findByPage(PageQuery query) {
        int offset = (query.getPage() - 1) * query.getSize();
        String countSql = "SELECT COUNT(*) FROM tb_stock_basic";
        long total = Optional.ofNullable(
                jdbcTemplate.queryForObject(countSql, new MapSqlParameterSource(), Long.class)
        ).orElse(0L);

        String sql = "SELECT * FROM tb_stock_basic ORDER BY stock_code LIMIT :size OFFSET :offset";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("size", query.getSize())
                .addValue("offset", offset);
        List<Stock> list = jdbcTemplate.query(sql, params, rowMapper).stream()
                .map(this::toDomain)
                .toList();

        return PageResult.<Stock>builder()
                .list(list)
                .total(total)
                .page(query.getPage())
                .size(query.getSize())
                .build();
    }

    @Override
    public List<Stock> findByIndustry(String industry) {
        String sql = "SELECT * FROM tb_stock_basic WHERE industry = :industry ORDER BY stock_code";
        List<StockEntity> results = jdbcTemplate.query(sql,
                new MapSqlParameterSource("industry", industry), rowMapper);
        return results.stream().map(this::toDomain).toList();
    }

    @Override
    public List<Stock> findByMarket(String market) {
        String sql = "SELECT * FROM tb_stock_basic WHERE market = :market ORDER BY stock_code";
        List<StockEntity> results = jdbcTemplate.query(sql,
                new MapSqlParameterSource("market", market), rowMapper);
        return results.stream().map(this::toDomain).toList();
    }

    @Override
    public StockId save(Stock stock) {
        String sql = """
                INSERT INTO tb_stock_basic (
                    id, stock_code, name, market, ts_code, full_name, exchange,
                    list_date, industry, area, total_shares, float_shares, created_at, updated_at
                ) VALUES (
                    :id, :stockCode, :name, :market, :tsCode, :fullName, :exchange,
                    :listDate, :industry, :area, :totalShares, :floatShares, :createdAt, :updatedAt
                )
                ON CONFLICT (id) DO UPDATE SET
                    stock_code = EXCLUDED.stock_code,
                    name = EXCLUDED.name,
                    market = EXCLUDED.market,
                    ts_code = EXCLUDED.ts_code,
                    full_name = EXCLUDED.full_name,
                    exchange = EXCLUDED.exchange,
                    list_date = EXCLUDED.list_date,
                    industry = EXCLUDED.industry,
                    area = EXCLUDED.area,
                    total_shares = EXCLUDED.total_shares,
                    float_shares = EXCLUDED.float_shares,
                    updated_at = EXCLUDED.updated_at
                """;
        StockEntity entity = toEntity(stock);
        SqlParameterSource params = new BeanPropertySqlParameterSource(entity);
        jdbcTemplate.update(sql, params);
        return stock.getId();
    }

    @Override
    public void deleteById(StockId id) {
        String sql = "DELETE FROM tb_stock_basic WHERE id = :id";
        jdbcTemplate.update(sql, new MapSqlParameterSource("id", id.getValue()));
    }

    private Stock toDomain(StockEntity entity) {
        return Stock.builder()
                .id(StockId.of(entity.getId()))
                .stockCode(entity.getStockCode())
                .name(entity.getName())
                .market(entity.getMarket())
                .tsCode(entity.getTsCode())
                .fullName(entity.getFullName())
                .exchange(entity.getExchange())
                .listDate(entity.getListDate())
                .industry(entity.getIndustry())
                .area(entity.getArea())
                .totalShares(entity.getTotalShares())
                .floatShares(entity.getFloatShares())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private StockEntity toEntity(Stock stock) {
        StockEntity entity = new StockEntity();
        entity.setId(stock.getId().getValue());
        entity.setStockCode(stock.getStockCode());
        entity.setName(stock.getName());
        entity.setMarket(stock.getMarket());
        entity.setTsCode(stock.getTsCode());
        entity.setFullName(stock.getFullName());
        entity.setExchange(stock.getExchange());
        entity.setListDate(stock.getListDate());
        entity.setIndustry(stock.getIndustry());
        entity.setArea(stock.getArea());
        entity.setTotalShares(stock.getTotalShares());
        entity.setFloatShares(stock.getFloatShares());
        entity.setCreatedAt(stock.getCreatedAt());
        entity.setUpdatedAt(stock.getUpdatedAt());
        return entity;
    }
}
