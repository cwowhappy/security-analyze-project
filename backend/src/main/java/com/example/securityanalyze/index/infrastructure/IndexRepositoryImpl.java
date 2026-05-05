package com.example.securityanalyze.index.infrastructure;

import com.example.securityanalyze.index.domain.IndexInfo;
import com.example.securityanalyze.index.domain.IndexRepository;
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
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class IndexRepositoryImpl implements IndexRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final String SELECT_SQL = """
            SELECT id, index_code, index_name, index_type, market,
                   base_date, base_point, component_count, publish_date, is_core, source,
                   created_at, updated_at
            FROM index_info
            """;

    private static final RowMapper<IndexInfo> ROW_MAPPER = (rs, rowNum) -> {
        IndexInfo index = new IndexInfo();
        index.setId(rs.getLong("id"));
        index.setIndexCode(rs.getString("index_code"));
        index.setIndexName(rs.getString("index_name"));
        index.setIndexType(rs.getString("index_type"));
        index.setMarket(rs.getString("market"));

        java.sql.Date baseDate = rs.getDate("base_date");
        if (baseDate != null) {
            index.setBaseDate(baseDate.toLocalDate());
        }

        BigDecimal basePoint = rs.getBigDecimal("base_point");
        index.setBasePoint(basePoint);

        int componentCount = rs.getInt("component_count");
        if (!rs.wasNull()) {
            index.setComponentCount(componentCount);
        }

        java.sql.Date publishDate = rs.getDate("publish_date");
        if (publishDate != null) {
            index.setPublishDate(publishDate.toLocalDate());
        }

        boolean isCore = rs.getBoolean("is_core");
        if (!rs.wasNull()) {
            index.setIsCore(isCore);
        }

        index.setSource(rs.getString("source"));

        java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            index.setCreatedAt(createdAt.toLocalDateTime());
        }

        java.sql.Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            index.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return index;
    };

    @Override
    public List<IndexInfo> findByKeyword(String keyword, int offset, int limit) {
        log.debug("查询指数, keyword={}, offset={}, limit={}", keyword, offset, limit);
        String sql = SELECT_SQL;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("offset", offset);
        params.addValue("limit", limit);

        if (keyword != null && !keyword.isBlank()) {
            sql += " WHERE index_code = :exactKeyword OR index_name ILIKE :keyword";
            params.addValue("exactKeyword", keyword.trim());
            params.addValue("keyword", "%" + keyword.trim() + "%");
        }

        sql += " ORDER BY index_code ASC LIMIT :limit OFFSET :offset";

        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    @Override
    public long countByKeyword(String keyword) {
        log.debug("统计指数数量, keyword={}", keyword);
        String sql = "SELECT COUNT(*) FROM index_info";
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (keyword != null && !keyword.isBlank()) {
            sql += " WHERE index_code = :exactKeyword OR index_name ILIKE :keyword";
            params.addValue("exactKeyword", keyword.trim());
            params.addValue("keyword", "%" + keyword.trim() + "%");
        }

        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public Optional<IndexInfo> findByIndexCode(String indexCode) {
        log.debug("根据指数代码查询, indexCode={}", indexCode);
        String sql = SELECT_SQL + " WHERE index_code = :indexCode";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("indexCode", indexCode);

        List<IndexInfo> results = jdbcTemplate.query(sql, params, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Optional<IndexInfo> findById(Long id) {
        log.debug("根据ID查询指数, id={}", id);
        String sql = SELECT_SQL + " WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);

        List<IndexInfo> results = jdbcTemplate.query(sql, params, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<IndexInfo> findAllByIndexCodes(List<String> indexCodes) {
        log.debug("批量查询指数, indexCodes={}", indexCodes);
        if (indexCodes == null || indexCodes.isEmpty()) {
            return List.of();
        }
        String sql = SELECT_SQL + " WHERE index_code IN (:indexCodes)";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("indexCodes", indexCodes);

        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    @Override
    public List<IndexInfo> findCoreByType(String indexType) {
        log.debug("查询核心指数, indexType={}", indexType);
        String sql = SELECT_SQL + " WHERE is_core = TRUE AND index_type = :indexType ORDER BY index_code";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("indexType", indexType);
        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }
}
