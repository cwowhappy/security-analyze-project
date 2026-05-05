package com.example.securityanalyze.index.infrastructure;

import com.example.securityanalyze.index.domain.IndexEtfMapping;
import com.example.securityanalyze.index.domain.IndexEtfMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class IndexEtfMappingRepositoryImpl implements IndexEtfMappingRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final String SELECT_SQL = """
            SELECT id, index_code, etf_code, relation_type, created_at
            FROM index_etf_mapping
            """;

    private static final RowMapper<IndexEtfMapping> ROW_MAPPER = (rs, rowNum) -> {
        IndexEtfMapping mapping = new IndexEtfMapping();
        mapping.setId(rs.getLong("id"));
        mapping.setIndexCode(rs.getString("index_code"));
        mapping.setEtfCode(rs.getString("etf_code"));
        mapping.setRelationType(rs.getString("relation_type"));

        java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            mapping.setCreatedAt(createdAt.toLocalDateTime());
        }

        return mapping;
    };

    @Override
    public List<IndexEtfMapping> findByIndexCode(String indexCode) {
        log.debug("根据指数代码查询映射, indexCode={}", indexCode);
        String sql = SELECT_SQL + " WHERE index_code = :indexCode";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("indexCode", indexCode);
        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    @Override
    public List<IndexEtfMapping> findByEtfCode(String etfCode) {
        log.debug("根据ETF代码查询映射, etfCode={}", etfCode);
        String sql = SELECT_SQL + " WHERE etf_code = :etfCode";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("etfCode", etfCode);
        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }
}
