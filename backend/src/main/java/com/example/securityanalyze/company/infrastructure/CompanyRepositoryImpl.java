package com.example.securityanalyze.company.infrastructure;

import com.example.securityanalyze.company.domain.Company;
import com.example.securityanalyze.company.domain.CompanyRepository;
import lombok.RequiredArgsConstructor;
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

@Repository
@RequiredArgsConstructor
public class CompanyRepositoryImpl implements CompanyRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final String SELECT_SQL = """
            SELECT id, unified_code, company_name, short_name, industry, region,
                   establish_date, registered_capital, created_at, updated_at
            FROM company
            """;

    private static final RowMapper<Company> ROW_MAPPER = new RowMapper<>() {
        @Override
        public Company mapRow(ResultSet rs, int rowNum) throws SQLException {
            Company company = new Company();
            company.setId(rs.getLong("id"));
            company.setUnifiedCode(rs.getString("unified_code"));
            company.setCompanyName(rs.getString("company_name"));
            company.setShortName(rs.getString("short_name"));
            company.setIndustry(rs.getString("industry"));
            company.setRegion(rs.getString("region"));

            java.sql.Date establishDate = rs.getDate("establish_date");
            if (establishDate != null) {
                company.setEstablishDate(establishDate.toLocalDate());
            }

            BigDecimal capital = rs.getBigDecimal("registered_capital");
            company.setRegisteredCapital(capital);

            java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                company.setCreatedAt(createdAt.toLocalDateTime());
            }

            java.sql.Timestamp updatedAt = rs.getTimestamp("updated_at");
            if (updatedAt != null) {
                company.setUpdatedAt(updatedAt.toLocalDateTime());
            }

            return company;
        }
    };

    @Override
    public List<Company> findByKeyword(String keyword, int offset, int limit) {
        String sql = SELECT_SQL;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("offset", offset);
        params.addValue("limit", limit);

        if (keyword != null && !keyword.isBlank()) {
            sql += " WHERE company_name ILIKE :keyword OR short_name ILIKE :keyword";
            params.addValue("keyword", "%" + keyword.trim() + "%");
        }

        sql += " ORDER BY id ASC LIMIT :limit OFFSET :offset";

        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    @Override
    public long countByKeyword(String keyword) {
        String sql = "SELECT COUNT(*) FROM company";
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (keyword != null && !keyword.isBlank()) {
            sql += " WHERE company_name ILIKE :keyword OR short_name ILIKE :keyword";
            params.addValue("keyword", "%" + keyword.trim() + "%");
        }

        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public Optional<Company> findById(Long id) {
        String sql = SELECT_SQL + " WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);

        List<Company> results = jdbcTemplate.query(sql, params, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Optional<Company> findByStockCode(String stockCode) {
        // 通过 company_security 关联查询
        String sql = SELECT_SQL + """
                WHERE id = (
                    SELECT company_id FROM company_security WHERE stock_code = :stockCode
                )
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("stockCode", stockCode);

        List<Company> results = jdbcTemplate.query(sql, params, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}
