package com.example.securityanalyze.industry.infrastructure;

import com.example.securityanalyze.industry.domain.CompanyIndustryMapping;
import com.example.securityanalyze.industry.domain.CompanyIndustryMappingRepository;
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

@Slf4j
@Repository
@RequiredArgsConstructor
public class CompanyIndustryMappingRepositoryImpl implements CompanyIndustryMappingRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final RowMapper<CompanyIndustryMapping> ROW_MAPPER = new RowMapper<>() {
        @Override
        public CompanyIndustryMapping mapRow(ResultSet rs, int rowNum) throws SQLException {
            CompanyIndustryMapping m = new CompanyIndustryMapping();
            m.setId(rs.getLong("id"));
            m.setCompanyId(rs.getLong("company_id"));
            m.setStandardCode(rs.getString("standard_code"));
            m.setLevel1Code(rs.getString("level1_code"));
            m.setLevel2Code(rs.getString("level2_code"));
            m.setPrimary(rs.getBoolean("is_primary"));
            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                m.setCreatedAt(createdAt.toLocalDateTime());
            }
            return m;
        }
    };

    @Override
    public void save(CompanyIndustryMapping mapping) {
        String sql = """
                INSERT INTO company_industry_mapping (company_id, standard_code, level1_code, level2_code, is_primary, created_at)
                VALUES (:companyId, :standardCode, :level1Code, :level2Code, :isPrimary, NOW())
                ON CONFLICT (company_id, standard_code, level2_code) DO UPDATE SET
                    level1_code = EXCLUDED.level1_code,
                    is_primary = EXCLUDED.is_primary
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("companyId", mapping.getCompanyId());
        params.addValue("standardCode", mapping.getStandardCode());
        params.addValue("level1Code", mapping.getLevel1Code());
        params.addValue("level2Code", mapping.getLevel2Code());
        params.addValue("isPrimary", mapping.getPrimary() != null ? mapping.getPrimary() : true);
        jdbcTemplate.update(sql, params);
    }

    @Override
    public void saveAll(List<CompanyIndustryMapping> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return;
        }
        String sql = """
                INSERT INTO company_industry_mapping (company_id, standard_code, level1_code, level2_code, is_primary, created_at)
                VALUES (:companyId, :standardCode, :level1Code, :level2Code, :isPrimary, NOW())
                ON CONFLICT (company_id, standard_code, level2_code) DO UPDATE SET
                    level1_code = EXCLUDED.level1_code,
                    is_primary = EXCLUDED.is_primary
                """;
        MapSqlParameterSource[] batch = mappings.stream()
                .map(m -> {
                    MapSqlParameterSource params = new MapSqlParameterSource();
                    params.addValue("companyId", m.getCompanyId());
                    params.addValue("standardCode", m.getStandardCode());
                    params.addValue("level1Code", m.getLevel1Code());
                    params.addValue("level2Code", m.getLevel2Code());
                    params.addValue("isPrimary", m.getPrimary() != null ? m.getPrimary() : true);
                    return params;
                })
                .toArray(MapSqlParameterSource[]::new);
        jdbcTemplate.batchUpdate(sql, batch);
    }

    @Override
    public List<CompanyIndustryMapping> findByCompanyId(Long companyId) {
        String sql = "SELECT * FROM company_industry_mapping WHERE company_id = :companyId ORDER BY standard_code, is_primary DESC";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("companyId", companyId);
        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    @Override
    public List<CompanyIndustryMapping> findByCompanyIdAndStandard(Long companyId, String standardCode) {
        String sql = "SELECT * FROM company_industry_mapping WHERE company_id = :companyId AND standard_code = :standardCode ORDER BY is_primary DESC";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("companyId", companyId);
        params.addValue("standardCode", standardCode);
        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    @Override
    public List<Long> findCompanyIdsByStandardAndLevel1(String standardCode, String level1Code) {
        String sql = "SELECT DISTINCT company_id FROM company_industry_mapping WHERE standard_code = :standardCode AND level1_code = :level1Code";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("standardCode", standardCode);
        params.addValue("level1Code", level1Code);
        return jdbcTemplate.queryForList(sql, params, Long.class);
    }

    @Override
    public List<Long> findCompanyIdsByStandardAndLevel2(String standardCode, String level2Code) {
        String sql = "SELECT DISTINCT company_id FROM company_industry_mapping WHERE standard_code = :standardCode AND level2_code = :level2Code";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("standardCode", standardCode);
        params.addValue("level2Code", level2Code);
        return jdbcTemplate.queryForList(sql, params, Long.class);
    }

    @Override
    public long countByStandardAndLevel1(String standardCode, String level1Code) {
        String sql = "SELECT COUNT(DISTINCT company_id) FROM company_industry_mapping WHERE standard_code = :standardCode AND level1_code = :level1Code";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("standardCode", standardCode);
        params.addValue("level1Code", level1Code);
        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public long countByStandardAndLevel2(String standardCode, String level2Code) {
        String sql = "SELECT COUNT(DISTINCT company_id) FROM company_industry_mapping WHERE standard_code = :standardCode AND level2_code = :level2Code";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("standardCode", standardCode);
        params.addValue("level2Code", level2Code);
        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public void deleteByCompanyId(Long companyId) {
        String sql = "DELETE FROM company_industry_mapping WHERE company_id = :companyId";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("companyId", companyId);
        jdbcTemplate.update(sql, params);
    }
}
