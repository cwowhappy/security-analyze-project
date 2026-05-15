package org.cwowhappy.securityanalyze.financial.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.cwowhappy.securityanalyze.financial.domain.model.FinancialIndicator;
import org.cwowhappy.securityanalyze.financial.domain.repository.FinancialIndicatorRepository;
import org.cwowhappy.securityanalyze.financial.infrastructure.persistence.entity.FinancialIndicatorEntity;
import org.cwowhappy.securityanalyze.financial.infrastructure.persistence.mapper.FinancialIndicatorRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 财务指标 JDBC 仓库实现。
 */
@Repository
@RequiredArgsConstructor
public class JdbcFinancialIndicatorRepository implements FinancialIndicatorRepository {

    private final JdbcTemplate jdbcTemplate;
    private final FinancialIndicatorRowMapper rowMapper;

    @Override
    @Transactional
    public void save(FinancialIndicator indicator) {
        String id = indicator.getId() != null ? indicator.getId() : UUID.randomUUID().toString().replace("-", "");
        String sql = """
            INSERT INTO tb_financial_indicator (
                id, stock_code, report_date, report_type,
                roe, roa, roic, gross_margin, net_margin, net_margin_excl,
                debt_ratio, current_ratio, quick_ratio, net_debt_ratio, equity_ratio,
                dso, dio, dpo, ccc, asset_turnover, fixed_asset_turnover,
                revenue_growth, np_parent_growth, np_excl_growth, cfo_growth,
                equity_growth, asset_growth,
                pe, pb, ps, peg, ev_ebitda, dividend_yield, market_cap,
                cfo_to_np, data_source, created_at, updated_at
            ) VALUES (
                ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW()
            )
            ON CONFLICT (stock_code, report_date, report_type) DO UPDATE SET
                roe = EXCLUDED.roe, roa = EXCLUDED.roa, roic = EXCLUDED.roic,
                gross_margin = EXCLUDED.gross_margin, net_margin = EXCLUDED.net_margin,
                net_margin_excl = EXCLUDED.net_margin_excl,
                debt_ratio = EXCLUDED.debt_ratio, current_ratio = EXCLUDED.current_ratio,
                quick_ratio = EXCLUDED.quick_ratio, net_debt_ratio = EXCLUDED.net_debt_ratio,
                equity_ratio = EXCLUDED.equity_ratio,
                dso = EXCLUDED.dso, dio = EXCLUDED.dio, dpo = EXCLUDED.dpo,
                ccc = EXCLUDED.ccc, asset_turnover = EXCLUDED.asset_turnover,
                fixed_asset_turnover = EXCLUDED.fixed_asset_turnover,
                revenue_growth = EXCLUDED.revenue_growth,
                np_parent_growth = EXCLUDED.np_parent_growth,
                np_excl_growth = EXCLUDED.np_excl_growth,
                cfo_growth = EXCLUDED.cfo_growth,
                equity_growth = EXCLUDED.equity_growth,
                asset_growth = EXCLUDED.asset_growth,
                pe = EXCLUDED.pe, pb = EXCLUDED.pb, ps = EXCLUDED.ps,
                peg = EXCLUDED.peg, ev_ebitda = EXCLUDED.ev_ebitda,
                dividend_yield = EXCLUDED.dividend_yield, market_cap = EXCLUDED.market_cap,
                cfo_to_np = EXCLUDED.cfo_to_np,
                data_source = EXCLUDED.data_source,
                updated_at = NOW()
            """;
        jdbcTemplate.update(sql,
            id, indicator.getStockCode(), indicator.getReportDate(), indicator.getReportType(),
            indicator.getRoe(), indicator.getRoa(), indicator.getRoic(), indicator.getGrossMargin(),
            indicator.getNetMargin(), indicator.getNetMarginExcl(),
            indicator.getDebtRatio(), indicator.getCurrentRatio(), indicator.getQuickRatio(),
            indicator.getNetDebtRatio(), indicator.getEquityRatio(),
            indicator.getDso(), indicator.getDio(), indicator.getDpo(), indicator.getCcc(),
            indicator.getAssetTurnover(), indicator.getFixedAssetTurnover(),
            indicator.getRevenueGrowth(), indicator.getNpParentGrowth(), indicator.getNpExclGrowth(),
            indicator.getCfoGrowth(), indicator.getEquityGrowth(), indicator.getAssetGrowth(),
            indicator.getPe(), indicator.getPb(), indicator.getPs(), indicator.getPeg(),
            indicator.getEvEbitda(), indicator.getDividendYield(), indicator.getMarketCap(),
            indicator.getCfoToNp(), indicator.getDataSource()
        );
    }

    @Override
    @Transactional
    public void saveAll(List<FinancialIndicator> indicators) {
        for (FinancialIndicator indicator : indicators) {
            save(indicator);
        }
    }

    @Override
    public List<FinancialIndicator> findByStockCode(String stockCode) {
        return findByStockCode(stockCode, null, 20);
    }

    @Override
    public List<FinancialIndicator> findByStockCode(String stockCode, String reportType) {
        return findByStockCode(stockCode, reportType, 20);
    }

    @Override
    public List<FinancialIndicator> findByStockCode(String stockCode, String reportType, int limit) {
        String sql;
        List<FinancialIndicatorEntity> entities;
        if (reportType != null) {
            sql = "SELECT * FROM tb_financial_indicator WHERE stock_code = ? AND report_type = ? ORDER BY report_date DESC LIMIT ?";
            entities = jdbcTemplate.query(sql, rowMapper, stockCode, reportType, limit);
        } else {
            sql = "SELECT * FROM tb_financial_indicator WHERE stock_code = ? ORDER BY report_date DESC LIMIT ?";
            entities = jdbcTemplate.query(sql, rowMapper, stockCode, limit);
        }
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<FinancialIndicator> findLatest(String stockCode, String reportType) {
        String sql = "SELECT * FROM tb_financial_indicator WHERE stock_code = ? AND report_type = ? ORDER BY report_date DESC LIMIT 1";
        List<FinancialIndicatorEntity> entities = jdbcTemplate.query(sql, rowMapper, stockCode, reportType);
        return entities.stream().findFirst().map(this::toDomain);
    }

    @Override
    public Optional<FinancialIndicator> findByStockCodeAndReportDate(String stockCode, LocalDate reportDate, String reportType) {
        String sql = "SELECT * FROM tb_financial_indicator WHERE stock_code = ? AND report_date = ? AND report_type = ?";
        List<FinancialIndicatorEntity> entities = jdbcTemplate.query(sql, rowMapper, stockCode, reportDate, reportType);
        return entities.stream().findFirst().map(this::toDomain);
    }

    @Override
    public List<FinancialIndicator> findLatestByStockCodes(List<String> stockCodes, String reportType) {
        if (stockCodes == null || stockCodes.isEmpty()) {
            return List.of();
        }
        String inClause = String.join(",", java.util.Collections.nCopies(stockCodes.size(), "?"));
        String sql = "SELECT DISTINCT ON (stock_code) * FROM tb_financial_indicator WHERE stock_code IN (" + inClause + ") AND report_type = ? ORDER BY stock_code, report_date DESC";
        Object[] params = new Object[stockCodes.size() + 1];
        for (int i = 0; i < stockCodes.size(); i++) {
            params[i] = stockCodes.get(i);
        }
        params[stockCodes.size()] = reportType;
        List<FinancialIndicatorEntity> entities = jdbcTemplate.query(sql, rowMapper, params);
        return entities.stream().map(this::toDomain).toList();
    }

    private FinancialIndicator toDomain(FinancialIndicatorEntity entity) {
        return FinancialIndicator.builder()
            .id(entity.getId())
            .stockCode(entity.getStockCode())
            .reportDate(entity.getReportDate())
            .reportType(entity.getReportType())
            .roe(entity.getRoe())
            .roa(entity.getRoa())
            .roic(entity.getRoic())
            .grossMargin(entity.getGrossMargin())
            .netMargin(entity.getNetMargin())
            .netMarginExcl(entity.getNetMarginExcl())
            .debtRatio(entity.getDebtRatio())
            .currentRatio(entity.getCurrentRatio())
            .quickRatio(entity.getQuickRatio())
            .netDebtRatio(entity.getNetDebtRatio())
            .equityRatio(entity.getEquityRatio())
            .dso(entity.getDso())
            .dio(entity.getDio())
            .dpo(entity.getDpo())
            .ccc(entity.getCcc())
            .assetTurnover(entity.getAssetTurnover())
            .fixedAssetTurnover(entity.getFixedAssetTurnover())
            .revenueGrowth(entity.getRevenueGrowth())
            .npParentGrowth(entity.getNpParentGrowth())
            .npExclGrowth(entity.getNpExclGrowth())
            .cfoGrowth(entity.getCfoGrowth())
            .equityGrowth(entity.getEquityGrowth())
            .assetGrowth(entity.getAssetGrowth())
            .pe(entity.getPe())
            .pb(entity.getPb())
            .ps(entity.getPs())
            .peg(entity.getPeg())
            .evEbitda(entity.getEvEbitda())
            .dividendYield(entity.getDividendYield())
            .marketCap(entity.getMarketCap())
            .cfoToNp(entity.getCfoToNp())
            .dataSource(entity.getDataSource())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}
