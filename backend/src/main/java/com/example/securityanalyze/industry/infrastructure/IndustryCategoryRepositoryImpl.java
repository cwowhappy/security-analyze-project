package com.example.securityanalyze.industry.infrastructure;

import com.example.securityanalyze.industry.domain.IndustryCategory;
import com.example.securityanalyze.industry.domain.IndustryCategoryRepository;
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
public class IndustryCategoryRepositoryImpl implements IndustryCategoryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final RowMapper<IndustryCategory> ROW_MAPPER = new RowMapper<>() {
        @Override
        public IndustryCategory mapRow(ResultSet rs, int rowNum) throws SQLException {
            IndustryCategory c = new IndustryCategory();
            c.setId(rs.getLong("id"));
            c.setStandardCode(rs.getString("standard_code"));
            c.setLevel(rs.getInt("level"));
            c.setCode(rs.getString("code"));
            c.setName(rs.getString("name"));
            c.setParentCode(rs.getString("parent_code"));
            c.setSortOrder(rs.getInt("sort_order"));
            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                c.setCreatedAt(createdAt.toLocalDateTime());
            }
            Timestamp updatedAt = rs.getTimestamp("updated_at");
            if (updatedAt != null) {
                c.setUpdatedAt(updatedAt.toLocalDateTime());
            }
            return c;
        }
    };

    @Override
    public void syncCategories(List<IndustryCategory> categories) {
        if (categories == null || categories.isEmpty()) {
            return;
        }
        String sql = """
                INSERT INTO industry_category (standard_code, level, code, name, parent_code, sort_order, updated_at)
                VALUES (:standardCode, :level, :code, :name, :parentCode, :sortOrder, NOW())
                ON CONFLICT (standard_code, level, code) DO UPDATE SET
                    name = EXCLUDED.name,
                    parent_code = EXCLUDED.parent_code,
                    sort_order = EXCLUDED.sort_order,
                    updated_at = EXCLUDED.updated_at
                """;
        MapSqlParameterSource[] batch = categories.stream()
                .map(c -> {
                    MapSqlParameterSource params = new MapSqlParameterSource();
                    params.addValue("standardCode", c.getStandardCode());
                    params.addValue("level", c.getLevel());
                    params.addValue("code", c.getCode());
                    params.addValue("name", c.getName());
                    params.addValue("parentCode", c.getParentCode());
                    params.addValue("sortOrder", c.getSortOrder() != null ? c.getSortOrder() : 0);
                    return params;
                })
                .toArray(MapSqlParameterSource[]::new);
        jdbcTemplate.batchUpdate(sql, batch);
        log.info("同步行业分类 {} 条", categories.size());
    }

    @Override
    public List<IndustryCategory> findByStandard(String standardCode) {
        String sql = "SELECT * FROM industry_category WHERE standard_code = :standardCode ORDER BY level, sort_order, name";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("standardCode", standardCode);
        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    @Override
    public List<IndustryCategory> findByStandardAndLevel(String standardCode, int level) {
        String sql = "SELECT * FROM industry_category WHERE standard_code = :standardCode AND level = :level ORDER BY sort_order, name";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("standardCode", standardCode);
        params.addValue("level", level);
        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    @Override
    public List<IndustryCategory> findByStandardAndParent(String standardCode, String parentCode) {
        String sql = "SELECT * FROM industry_category WHERE standard_code = :standardCode AND parent_code = :parentCode ORDER BY sort_order, name";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("standardCode", standardCode);
        params.addValue("parentCode", parentCode);
        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    @Override
    public Optional<IndustryCategory> findByCode(String standardCode, String code) {
        String sql = "SELECT * FROM industry_category WHERE standard_code = :standardCode AND code = :code";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("standardCode", standardCode);
        params.addValue("code", code);
        List<IndustryCategory> results = jdbcTemplate.query(sql, params, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<IndustryCategory> findByStandardAndLevelWithCount(String standardCode, int level) {
        String sql = """
                SELECT c.*, COUNT(DISTINCT m.company_id) AS company_count
                FROM industry_category c
                LEFT JOIN company_industry_mapping m ON c.standard_code = m.standard_code
                    AND c.code = (CASE WHEN c.level = 1 THEN m.level1_code ELSE m.level2_code END)
                WHERE c.standard_code = :standardCode AND c.level = :level
                GROUP BY c.id, c.standard_code, c.level, c.code, c.name, c.parent_code, c.sort_order, c.created_at, c.updated_at
                ORDER BY company_count DESC, c.sort_order, c.name
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("standardCode", standardCode);
        params.addValue("level", level);
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            IndustryCategory c = ROW_MAPPER.mapRow(rs, rowNum);
            c.setCompanyCount(rs.getInt("company_count"));
            return c;
        });
    }
}
