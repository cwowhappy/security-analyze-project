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
            SELECT id, stock_code, stock_name, industry, region,
                   establish_date, registered_capital, listing_date, market,
                   created_at, updated_at
            FROM company
            """;

    private static final RowMapper<Company> ROW_MAPPER = new RowMapper<>() {
        @Override
        public Company mapRow(ResultSet rs, int rowNum) throws SQLException {
            Company company = new Company();
            company.setId(rs.getLong("id"));
            company.setStockCode(rs.getString("stock_code"));
            company.setStockName(rs.getString("stock_name"));
            company.setIndustry(rs.getString("industry"));
            company.setRegion(rs.getString("region"));

            java.sql.Date establishDate = rs.getDate("establish_date");
            if (establishDate != null) {
                company.setEstablishDate(establishDate.toLocalDate());
            }

            BigDecimal capital = rs.getBigDecimal("registered_capital");
            company.setRegisteredCapital(capital);

            java.sql.Date listingDate = rs.getDate("listing_date");
            if (listingDate != null) {
                company.setListingDate(listingDate.toLocalDate());
            }

            company.setMarket(rs.getString("market"));

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
            sql += " WHERE stock_code ILIKE :keyword OR stock_name ILIKE :keyword";
            params.addValue("keyword", "%" + keyword.trim() + "%");
        }

        sql += " ORDER BY stock_code ASC LIMIT :limit OFFSET :offset";

        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    @Override
    public long countByKeyword(String keyword) {
        String sql = "SELECT COUNT(*) FROM company";
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (keyword != null && !keyword.isBlank()) {
            sql += " WHERE stock_code ILIKE :keyword OR stock_name ILIKE :keyword";
            params.addValue("keyword", "%" + keyword.trim() + "%");
        }

        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public Optional<Company> findByStockCode(String stockCode) {
        String sql = SELECT_SQL + " WHERE stock_code = :stockCode";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("stockCode", stockCode);

        List<Company> results = jdbcTemplate.query(sql, params, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}
