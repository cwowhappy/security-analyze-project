package com.example.securityanalyze.company.infrastructure;

import com.example.securityanalyze.company.domain.CompanySecurity;
import com.example.securityanalyze.company.domain.CompanySecurityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Repository
@RequiredArgsConstructor
public class CompanySecurityRepositoryImpl implements CompanySecurityRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final String SELECT_SQL = """
            SELECT id, company_id, stock_code, stock_name, market,
                   security_type, listing_date, listing_status, created_at, updated_at,
                   is_deleted, deleted_at
            FROM company_security
            WHERE is_deleted = FALSE
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

            security.setIsDeleted(rs.getBoolean("is_deleted"));

            java.sql.Timestamp deletedAt = rs.getTimestamp("deleted_at");
            if (deletedAt != null) {
                security.setDeletedAt(deletedAt.toLocalDateTime());
            }

            return security;
        }
    };

    @Override
    public List<CompanySecurity> findByCompanyId(Long companyId) {
        log.debug("根据公司ID查询证券, companyId={}", companyId);
        String sql = SELECT_SQL + " AND company_id = :companyId ORDER BY stock_code ASC";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("companyId", companyId);

        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    @Override
    public List<CompanySecurity> findByCompanyIds(List<Long> companyIds) {
        log.debug("根据公司ID列表批量查询证券, companyIds={}", companyIds);
        if (companyIds == null || companyIds.isEmpty()) {
            return List.of();
        }
        String sql = SELECT_SQL + " AND company_id IN (:companyIds) ORDER BY stock_code ASC";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("companyIds", companyIds);

        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    @Override
    public Optional<CompanySecurity> findByStockCode(String stockCode) {
        log.debug("根据股票代码查询证券, stockCode={}", stockCode);
        String sql = SELECT_SQL + " AND stock_code = :stockCode";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("stockCode", stockCode);

        List<CompanySecurity> results = jdbcTemplate.query(sql, params, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<CompanySecurity> findByKeyword(String keyword, int offset, int limit) {
        log.debug("搜索证券, keyword={}, offset={}, limit={}", keyword, offset, limit);
        String sql = SELECT_SQL;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("offset", offset);
        params.addValue("limit", limit);

        if (keyword != null && !keyword.isBlank()) {
            sql += " AND (stock_code = :keyword OR stock_name ILIKE :prefix)";
            params.addValue("keyword", keyword.trim());
            params.addValue("prefix", keyword.trim() + "%");
        }

        sql += " ORDER BY stock_code ASC LIMIT :limit OFFSET :offset";

        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    @Override
    public long countByKeyword(String keyword) {
        log.debug("统计证券数量, keyword={}", keyword);
        String sql = "SELECT COUNT(*) FROM company_security WHERE is_deleted = FALSE";
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (keyword != null && !keyword.isBlank()) {
            sql += " AND (stock_code = :keyword OR stock_name ILIKE :prefix)";
            params.addValue("keyword", keyword.trim());
            params.addValue("prefix", keyword.trim() + "%");
        }

        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null ? count : 0L;
    }
}
