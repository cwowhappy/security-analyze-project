package org.cwowhappy.securityanalyze.financial.application.service;

import lombok.RequiredArgsConstructor;
import org.cwowhappy.securityanalyze.financial.application.dto.DupontAnalysisDTO;
import org.cwowhappy.securityanalyze.financial.application.dto.FinancialIndicatorDTO;
import org.cwowhappy.securityanalyze.financial.application.dto.PeerComparisonDTO;
import org.cwowhappy.securityanalyze.financial.application.dto.TrendDataDTO;
import org.cwowhappy.securityanalyze.financial.domain.model.FinancialBalance;
import org.cwowhappy.securityanalyze.financial.domain.model.FinancialIncome;
import org.cwowhappy.securityanalyze.financial.domain.model.FinancialIndicator;
import org.cwowhappy.securityanalyze.financial.domain.repository.FinancialBalanceRepository;
import org.cwowhappy.securityanalyze.financial.domain.repository.FinancialIncomeRepository;
import org.cwowhappy.securityanalyze.financial.domain.repository.FinancialIndicatorRepository;
import org.cwowhappy.securityanalyze.stock.domain.model.Stock;
import org.cwowhappy.securityanalyze.stock.domain.repository.StockRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 财务分析应用服务（趋势、杜邦、同业对比）。
 */
@Service
@RequiredArgsConstructor
public class FinancialAnalysisAppService {

    private final FinancialIndicatorRepository indicatorRepository;
    private final FinancialIncomeRepository incomeRepository;
    private final FinancialBalanceRepository balanceRepository;
    private final StockRepository stockRepository;

    public List<TrendDataDTO> getTrend(String stockCode, List<String> metrics, String reportType, int periods) {
        List<FinancialIndicator> indicators = indicatorRepository.findByStockCode(stockCode, reportType, periods);
        List<TrendDataDTO> results = new ArrayList<>();

        for (String metric : metrics) {
            List<TrendDataDTO.TrendPoint> points = new ArrayList<>();
            for (FinancialIndicator indicator : indicators) {
                BigDecimal value = extractMetric(indicator, metric);
                if (value != null) {
                    points.add(TrendDataDTO.TrendPoint.builder()
                        .reportDate(indicator.getReportDate())
                        .value(value)
                        .build());
                }
            }
            results.add(TrendDataDTO.builder()
                .stockCode(stockCode)
                .metric(metric)
                .data(points)
                .build());
        }
        return results;
    }

    public DupontAnalysisDTO getDupontAnalysis(String stockCode, LocalDate reportDate, String reportType) {
        FinancialIndicator indicator = indicatorRepository
            .findByStockCodeAndReportDate(stockCode, reportDate, reportType)
            .orElse(null);

        FinancialIncome income = incomeRepository
            .findByStockCodeAndReportDate(stockCode, reportDate, reportType)
            .orElse(null);

        FinancialBalance balance = balanceRepository
            .findByStockCodeAndReportDate(stockCode, reportDate, reportType)
            .orElse(null);

        if (income == null || balance == null) {
            return null;
        }

        // 净利率 = 净利润 / 营收
        BigDecimal netMargin = safeDiv(income.getNetProfit(), income.getRevenue());

        // 资产周转率 = 营收 / 总资产
        BigDecimal assetTurnover = safeDiv(income.getRevenue(), balance.getTotalAssets());

        // 权益乘数 = 总资产 / 净资产
        BigDecimal equityMultiplier = safeDiv(balance.getTotalAssets(), balance.getTotalEquity());

        // ROE = 净利率 × 资产周转率 × 权益乘数
        BigDecimal roe = null;
        if (netMargin != null && assetTurnover != null && equityMultiplier != null) {
            roe = netMargin.multiply(assetTurnover).multiply(equityMultiplier)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
        }

        return DupontAnalysisDTO.builder()
            .stockCode(stockCode)
            .reportDate(reportDate)
            .reportType(reportType)
            .roe(roe != null ? roe : (indicator != null ? indicator.getRoe() : null))
            .netMargin(netMargin != null ? netMargin.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP) : null)
            .assetTurnover(assetTurnover)
            .equityMultiplier(equityMultiplier)
            .build();
    }

    public PeerComparisonDTO getPeerComparison(String stockCode, String metric, String reportType) {
        Optional<Stock> stockOpt = stockRepository.findByStockCode(stockCode);
        if (stockOpt.isEmpty() || stockOpt.get().getIndustry() == null) {
            return null;
        }

        String industry = stockOpt.get().getIndustry();
        List<Stock> peers = stockRepository.findByIndustry(industry);
        List<String> peerCodes = peers.stream().map(Stock::getStockCode).toList();

        if (peerCodes.isEmpty()) {
            return null;
        }

        List<FinancialIndicator> indicators = indicatorRepository.findLatestByStockCodes(peerCodes, reportType);

        List<PeerComparisonDTO.PeerItem> peerItems = new ArrayList<>();
        List<BigDecimal> values = new ArrayList<>();
        BigDecimal stockValue = null;

        for (FinancialIndicator indicator : indicators) {
            BigDecimal value = extractMetric(indicator, metric);
            if (value == null) continue;

            values.add(value);

            Stock peerStock = peers.stream()
                .filter(s -> s.getStockCode().equals(indicator.getStockCode()))
                .findFirst()
                .orElse(null);

            peerItems.add(PeerComparisonDTO.PeerItem.builder()
                .stockCode(indicator.getStockCode())
                .stockName(peerStock != null ? peerStock.getName() : indicator.getStockCode())
                .value(value)
                .build());

            if (indicator.getStockCode().equals(stockCode)) {
                stockValue = value;
            }
        }

        if (values.isEmpty()) {
            return null;
        }

        Collections.sort(values);
        BigDecimal min = values.get(0);
        BigDecimal max = values.get(values.size() - 1);
        BigDecimal median = calcMedian(values);
        BigDecimal avg = calcAvg(values);

        String metricName = metricName(metric);

        return PeerComparisonDTO.builder()
            .stockCode(stockCode)
            .metric(metric)
            .metricName(metricName)
            .stockValue(stockValue)
            .industryAvg(avg)
            .industryMedian(median)
            .industryMax(max)
            .industryMin(min)
            .peers(peerItems)
            .build();
    }

    private BigDecimal extractMetric(FinancialIndicator indicator, String metric) {
        if (indicator == null || metric == null) return null;
        return switch (metric) {
            case "roe" -> indicator.getRoe();
            case "roa" -> indicator.getRoa();
            case "roic" -> indicator.getRoic();
            case "grossMargin" -> indicator.getGrossMargin();
            case "netMargin" -> indicator.getNetMargin();
            case "netMarginExcl" -> indicator.getNetMarginExcl();
            case "revenueGrowth" -> indicator.getRevenueGrowth();
            case "npParentGrowth" -> indicator.getNpParentGrowth();
            case "npExclGrowth" -> indicator.getNpExclGrowth();
            case "debtRatio" -> indicator.getDebtRatio();
            case "currentRatio" -> indicator.getCurrentRatio();
            case "quickRatio" -> indicator.getQuickRatio();
            case "assetTurnover" -> indicator.getAssetTurnover();
            case "pe" -> indicator.getPe();
            case "pb" -> indicator.getPb();
            case "ps" -> indicator.getPs();
            case "cfoToNp" -> indicator.getCfoToNp();
            default -> null;
        };
    }

    private String metricName(String metric) {
        return switch (metric) {
            case "roe" -> "ROE";
            case "roa" -> "ROA";
            case "roic" -> "ROIC";
            case "grossMargin" -> "毛利率";
            case "netMargin" -> "净利率";
            case "netMarginExcl" -> "扣非净利率";
            case "revenueGrowth" -> "营收增速";
            case "npParentGrowth" -> "归母净利增速";
            case "npExclGrowth" -> "扣非净利增速";
            case "debtRatio" -> "资产负债率";
            case "currentRatio" -> "流动比率";
            case "quickRatio" -> "速动比率";
            case "assetTurnover" -> "总资产周转率";
            case "pe" -> "PE-TTM";
            case "pb" -> "PB";
            case "ps" -> "PS";
            case "cfoToNp" -> "经营现金流/净利润";
            default -> metric;
        };
    }

    private BigDecimal calcMedian(List<BigDecimal> sortedValues) {
        int size = sortedValues.size();
        if (size == 0) return null;
        if (size % 2 == 1) {
            return sortedValues.get(size / 2).setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal a = sortedValues.get(size / 2 - 1);
        BigDecimal b = sortedValues.get(size / 2);
        return a.add(b).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcAvg(List<BigDecimal> values) {
        if (values.isEmpty()) return null;
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal safeDiv(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return numerator.divide(denominator, 4, RoundingMode.HALF_UP);
    }
}
