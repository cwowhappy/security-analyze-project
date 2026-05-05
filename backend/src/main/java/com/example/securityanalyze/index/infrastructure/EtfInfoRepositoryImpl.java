package com.example.securityanalyze.index.infrastructure;

import com.example.securityanalyze.index.domain.EtfInfo;
import com.example.securityanalyze.index.domain.EtfInfoRepository;
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
public class EtfInfoRepositoryImpl implements EtfInfoRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final String SELECT_SQL = """
            SELECT id, etf_code, etf_name, tracking_index_code,
                   management_fee, fund_size, establish_date, market, source,
                   created_at, updated_at
            FROM etf_info
            """;

    private static final RowMapper<EtfInfo> ROW_MAPPER = (rs, rowNum) -> {
        EtfInfo etf = new EtfInfo();
        etf.setId(rs.getLong("id"));
        etf.setEtfCode(rs.getString("etf_code"));
        etf.setEtfName(rs.getString("etf_name"));
        etf.setTrackingIndexCode(rs.getString("tracking_index_code"));
        etf.setManagementFee(rs.getBigDecimal("management_fee"));
        etf.setFundSize(rs.getBigDecimal("fund_size"));

        java.sql.Date establishDate = rs.getDate("establish_date");
        if (establishDate != null) {
            etf.setEstablishDate(establishDate.toLocalDate());
        }

        etf.setMarket(rs.getString("market"));
        etf.setSource(rs.getString("source"));

        java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            etf.setCreatedAt(createdAt.toLocalDateTime());
        }

        java.sql.Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            etf.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return etf;
    };

    @Override
    public List<EtfInfo> findByTrackingIndexCode(String indexCode) {
        log.debug("根据跟踪指数查询ETF, indexCode={}", indexCode);
        String sql = SELECT_SQL + " WHERE tracking_index_code = :indexCode";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("indexCode", indexCode);
        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    @Override
    public Optional<EtfInfo> findByEtfCode(String etfCode) {
        log.debug("根据ETF代码查询, etfCode={}", etfCode);
        String sql = SELECT_SQL + " WHERE etf_code = :etfCode";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("etfCode", etfCode);
        List<EtfInfo> results = jdbcTemplate.query(sql, params, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<EtfInfo> findByEtfCodes(List<String> etfCodes) {
        log.debug("批量查询ETF, etfCodes={}", etfCodes);
        if (etfCodes == null || etfCodes.isEmpty()) {
            return List.of();
        }
        String sql = SELECT_SQL + " WHERE etf_code IN (:etfCodes)";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("etfCodes", etfCodes);
        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }
}
