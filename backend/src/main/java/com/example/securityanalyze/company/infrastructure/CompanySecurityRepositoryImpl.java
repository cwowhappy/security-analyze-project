package com.example.securityanalyze.company.infrastructure;

import com.example.securityanalyze.company.domain.CompanySecurity;
import com.example.securityanalyze.company.domain.CompanySecurityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CompanySecurityRepositoryImpl implements CompanySecurityRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final String SELECT_SQL = """
            SELECT id, company_id, stock_code, stock_name, market,
                   security_type, listing_date, listing_status, created_at, updated_at
            FROM company_security
            """;

    private static final RowMapper<CompanySecurity> ROW_MAPPER = new RowMapper<>() {
        @Override
        public CompanySecurity mapRow(ResultSet rs, int rowNum) throws SQLException {
            CompanySecurity security = new CompanySecurity();
            security.setId(rs.getLong("id"));
            security.setCompanyId(rs.getLong("company_id"));
            security.setStockCode(rs.getString("stock_code"));
            security.setStockName(rs.getString("stock_name"));
            security.setMarket(rs.getString("market"));
            security.setSecurityType(rs.getString("security_type"));

            java.sql.Date listingDate = rs.getDate("listing_date");
            if (listingDate != null) {
                security.setListingDate(listingDate.toLocalDate());
            }

            security.setListingStatus(rs.getString("listing_status"));

            java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                security.setCreatedAt(createdAt.toLocalDateTime());
            }

            java.sql.Timestamp updatedAt = rs.getTimestamp("updated_at");
            if (updatedAt != null) {
                security.setUpdatedAt(updatedAt.toLocalDateTime());
            }

            return security;
        }
    };

    @Override
    public List<CompanySecurity> findByCompanyId(Long companyId) {
        String sql = SELECT_SQL + " WHERE company_id = :companyId ORDER BY stock_code ASC";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("companyId", companyId);

        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    @Override
    public Optional<CompanySecurity> findByStockCode(String stockCode) {
        String sql = SELECT_SQL + " WHERE stock_code = :stockCode";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("stockCode", stockCode);

        List<CompanySecurity> results = jdbcTemplate.query(sql, params, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<CompanySecurity> findByKeyword(String keyword, int offset, int limit) {
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
        String sql = "SELECT COUNT(*) FROM company_security";
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (keyword != null && !keyword.isBlank()) {
            sql += " WHERE stock_code ILIKE :keyword OR stock_name ILIKE :keyword";
            params.addValue("keyword", "%" + keyword.trim() + "%");
        }

        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null ? count : 0L;
    }
}
