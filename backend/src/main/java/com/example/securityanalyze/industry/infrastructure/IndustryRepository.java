package com.example.securityanalyze.industry.infrastructure;

import com.example.securityanalyze.collector.api.CollectorOverviewItem;
import com.example.securityanalyze.company.api.CompanyListItem;
import com.example.securityanalyze.industry.api.IndustryListItem;
import com.example.securityanalyze.industry.api.TrendDataPoint;
import lombok.RequiredArgsConstructor;
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

@Repository
@RequiredArgsConstructor
public class IndustryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final RowMapper<IndustryListItem> INDUSTRY_ROW_MAPPER = new RowMapper<>() {
        @Override
        public IndustryListItem mapRow(ResultSet rs, int rowNum) throws SQLException {
            IndustryListItem item = new IndustryListItem();
            item.setIndustryName(rs.getString("industry"));
            item.setCompanyCount(rs.getInt("cnt"));
            return item;
        }
    };

    private static final RowMapper<CompanyListItem> COMPANY_ROW_MAPPER = new RowMapper<>() {
        @Override
        public CompanyListItem mapRow(ResultSet rs, int rowNum) throws SQLException {
            CompanyListItem item = new CompanyListItem();
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

    public List<IndustryListItem> findIndustries() {
        String sql = """
                SELECT industry, COUNT(*) AS cnt
                FROM company
                WHERE industry IS NOT NULL AND industry != ''
                GROUP BY industry
                ORDER BY cnt DESC
                """;
        return jdbcTemplate.query(sql, INDUSTRY_ROW_MAPPER);
    }

    public List<CompanyListItem> findCompaniesByIndustry(String industry, int offset, int limit) {
        String sql = """
                SELECT cs.stock_code, cs.stock_name, c.industry, c.region, cs.listing_date, cs.market
                FROM company c
                JOIN company_security cs ON cs.company_id = c.id
                WHERE c.industry = :industry
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
        String sql = """
                SELECT COUNT(*) FROM company
                WHERE industry = :industry
                """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("industry", industry);
        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null ? count : 0L;
    }
}
