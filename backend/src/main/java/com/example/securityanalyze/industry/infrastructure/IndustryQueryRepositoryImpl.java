package com.example.securityanalyze.industry.infrastructure;

import com.example.securityanalyze.industry.domain.IndustryCompany;
import com.example.securityanalyze.industry.domain.IndustryQueryRepository;
import com.example.securityanalyze.industry.domain.IndustrySummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class IndustryQueryRepositoryImpl implements IndustryQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final RowMapper<IndustrySummary> INDUSTRY_ROW_MAPPER = new RowMapper<>() {
        @Override
        public IndustrySummary mapRow(ResultSet rs, int rowNum) throws SQLException {
            IndustrySummary item = new IndustrySummary();
            item.setIndustryName(rs.getString("industry"));
            item.setCompanyCount(rs.getInt("cnt"));
            return item;
        }
    };

    private static final RowMapper<IndustryCompany> COMPANY_ROW_MAPPER = new RowMapper<>() {
        @Override
        public IndustryCompany mapRow(ResultSet rs, int rowNum) throws SQLException {
            IndustryCompany item = new IndustryCompany();
            item.setStockCode(rs.getString("stock_code"));
            item.setStockName(rs.getString("stock_name"));
            item.setIndustry(rs.getString("industry"));
            item.setRegion(rs.getString("region"));

            Date listingDate = rs.getDate("listing_date");
            if (listingDate != null) {
                item.setListingDate(listingDate.toLocalDate());
            }

            item.setMarket(rs.getString("market"));
            return item;
        }
    };

    public List<IndustrySummary> findIndustries() {
        log.debug("查询行业列表");
        String sql = """
                SELECT industry, COUNT(*) AS cnt
                FROM company
                WHERE industry IS NOT NULL AND industry != '' AND is_deleted = FALSE
                GROUP BY industry
                ORDER BY cnt DESC
                """;
        return jdbcTemplate.query(sql, INDUSTRY_ROW_MAPPER);
    }

    public List<IndustryCompany> findCompaniesByIndustry(String industry, int offset, int limit) {
        log.debug("根据行业查询公司, industry={}, offset={}, limit={}", industry, offset, limit);
        String sql = """
                SELECT cs.stock_code, cs.stock_name, c.industry, c.region, cs.listing_date, cs.market
                FROM company c
                JOIN company_security cs ON cs.company_id = c.id
                WHERE c.industry = :industry AND c.is_deleted = FALSE AND cs.is_deleted = FALSE
                ORDER BY cs.stock_code ASC
                LIMIT :limit OFFSET :offset
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("industry", industry);
        params.addValue("offset", offset);
        params.addValue("limit", limit);
        return jdbcTemplate.query(sql, params, COMPANY_ROW_MAPPER);
    }

    public long countCompaniesByIndustry(String industry) {
        log.debug("统计行业公司数量, industry={}", industry);
        String sql = """
                SELECT COUNT(*) FROM company
                WHERE industry = :industry AND is_deleted = FALSE
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("industry", industry);
        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null ? count : 0L;
    }
}
