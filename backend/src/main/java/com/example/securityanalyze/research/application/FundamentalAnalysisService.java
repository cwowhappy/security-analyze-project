package com.example.securityanalyze.research.application;

import com.example.securityanalyze.common.util.PageUtils;
import com.example.securityanalyze.research.api.AnnualMetricDto;
import com.example.securityanalyze.research.api.FundamentalOverviewResponse;
import com.example.securityanalyze.research.api.FundamentalScreenResponse;
import com.example.securityanalyze.research.api.IndustryPeersResponse;
import com.example.securityanalyze.research.api.PeerMetricDto;
import com.example.securityanalyze.research.api.ScreenCompanyItemResponse;
import com.example.securityanalyze.research.domain.AnnualMetric;
import com.example.securityanalyze.research.domain.FundamentalMetrics;
import com.example.securityanalyze.research.domain.FundamentalMetricsRepository;
import com.example.securityanalyze.research.domain.PeerMetric;
import com.example.securityanalyze.research.domain.ScreenCompanyItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FundamentalAnalysisService {

    private final FundamentalMetricsRepository fundamentalMetricsRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public Optional<FundamentalOverviewResponse> getOverview(String stockCode) {
        log.debug("获取基本面概览, stockCode={}", stockCode);
        Optional<FundamentalMetrics> metricsOpt = fundamentalMetricsRepository.findByStockCode(stockCode, 5);
        return metricsOpt.map(this::toOverviewResponse);
    }

    public FundamentalScreenResponse screenCompanies(
            String keyword, String industry, String market, int page, int size) {
        log.debug("筛选公司, keyword={}, industry={}, market={}, page={}, size={}",
                keyword, industry, market, page, size);
        int[] normalized = PageUtils.normalize(page, size);
        int offset = normalized[0] * normalized[1];

        List<ScreenCompanyItem> items = fundamentalMetricsRepository.screenCompanies(
                keyword, industry, market, offset, normalized[1]);
        long total = fundamentalMetricsRepository.countScreenCompanies(keyword, industry, market);

        FundamentalScreenResponse response = new FundamentalScreenResponse();
        response.setItems(items.stream().map(this::toScreenResponseItem).collect(Collectors.toList()));
        response.setTotal(total);
        response.setPage(normalized[0]);
        response.setSize(normalized[1]);
        return response;
    }

    public IndustryPeersResponse getIndustryPeers(String stockCode) {
        log.debug("获取同行业对比, stockCode={}", stockCode);
        List<PeerMetric> peers = fundamentalMetricsRepository.findIndustryPeers(stockCode);
        IndustryPeersResponse response = new IndustryPeersResponse();
        response.setPeers(peers.stream().map(this::toPeerDto).collect(Collectors.toList()));
        return response;
    }

    private FundamentalOverviewResponse toOverviewResponse(FundamentalMetrics metrics) {
        FundamentalOverviewResponse response = new FundamentalOverviewResponse();
        response.setStockCode(metrics.getStockCode());
        response.setStockName(metrics.getStockName());
        response.setIndustry(metrics.getIndustry());
        response.setMarket(metrics.getMarket());
        response.setMetrics(metrics.getAnnualMetrics().stream()
                .map(this::toAnnualMetricDto)
                .collect(Collectors.toList()));
        return response;
    }

    private AnnualMetricDto toAnnualMetricDto(AnnualMetric metric) {
        AnnualMetricDto dto = new AnnualMetricDto();
        dto.setReportDate(metric.getReportDate() != null ? metric.getReportDate().format(DATE_FORMATTER) : null);
        dto.setReportYear(metric.getReportYear());

        // 原始字段直接复制
        dto.setTotalRevenue(metric.getTotalRevenue());
        dto.setOperateIncome(metric.getOperateIncome());
        dto.setOperateCost(metric.getOperateCost());
        dto.setParentNetProfit(metric.getParentNetProfit());
        dto.setTotalAssets(metric.getTotalAssets());
        dto.setTotalLiabilities(metric.getTotalLiabilities());
        dto.setTotalEquity(metric.getTotalEquity());
        dto.setOperatingCashFlow(metric.getOperatingCashFlow());
        dto.setInvestingCashFlow(metric.getInvestingCashFlow());
        dto.setFinancingCashFlow(metric.getFinancingCashFlow());
        dto.setEndCce(metric.getEndCce());
        dto.setSaleExpense(metric.getSaleExpense());
        dto.setManageExpense(metric.getManageExpense());
        dto.setResearchExpense(metric.getResearchExpense());
        dto.setFinanceExpense(metric.getFinanceExpense());

        // 计算衍生指标
        // 毛利率 = (operate_income - operate_cost) / operate_income * 100
        dto.setGrossMargin(safePercentage(
                subtract(metric.getOperateIncome(), metric.getOperateCost()),
                metric.getOperateIncome()));

        // 净利率 = parent_net_profit / operate_income * 100
        dto.setNetMargin(safePercentage(
                metric.getParentNetProfit(),
                metric.getOperateIncome()));

        // ROE = parent_net_profit / total_equity * 100
        dto.setRoe(safePercentage(
                metric.getParentNetProfit(),
                metric.getTotalEquity()));

        // 资产负债率 = total_liabilities / total_assets * 100
        dto.setDebtRatio(safePercentage(
                metric.getTotalLiabilities(),
                metric.getTotalAssets()));

        // 期间费用率 = (sale + manage + research + finance) / operate_income * 100
        BigDecimal totalPeriodExpense = sum(
                metric.getSaleExpense(),
                metric.getManageExpense(),
                metric.getResearchExpense(),
                metric.getFinanceExpense());
        dto.setPeriodExpenseRate(safePercentage(totalPeriodExpense, metric.getOperateIncome()));

        // 经营现金流/净利润比 = operating_cash_flow / parent_net_profit * 100
        dto.setCashflowProfitRatio(safePercentage(
                metric.getOperatingCashFlow(),
                metric.getParentNetProfit()));

        return dto;
    }

    private ScreenCompanyItemResponse toScreenResponseItem(ScreenCompanyItem item) {
        ScreenCompanyItemResponse dto = new ScreenCompanyItemResponse();
        dto.setStockCode(item.getStockCode());
        dto.setStockName(item.getStockName());
        dto.setIndustry(item.getIndustry());
        dto.setMarket(item.getMarket());
        dto.setLatestRevenue(item.getLatestRevenue());
        dto.setLatestProfit(item.getLatestProfit());
        return dto;
    }

    private PeerMetricDto toPeerDto(PeerMetric peer) {
        PeerMetricDto dto = new PeerMetricDto();
        dto.setStockCode(peer.getStockCode());
        dto.setStockName(peer.getStockName());
        dto.setIndustry(peer.getIndustry());
        dto.setTotalRevenue(peer.getTotalRevenue());
        dto.setParentNetProfit(peer.getParentNetProfit());
        dto.setRoe(peer.getRoe());
        dto.setDebtRatio(peer.getDebtRatio());
        return dto;
    }

    // ========== 安全计算工具方法 ==========

    private static BigDecimal safePercentage(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null) {
            return null;
        }
        BigDecimal result = safeDivide(numerator, denominator, 4);
        return result != null ? result.multiply(BigDecimal.valueOf(100)) : null;
    }

    private static BigDecimal safeDivide(BigDecimal numerator, BigDecimal denominator, int scale) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return numerator.divide(denominator, scale, RoundingMode.HALF_UP);
    }

    private static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return null;
        }
        return a.subtract(b);
    }

    private static BigDecimal sum(BigDecimal... values) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal v : values) {
            if (v != null) {
                sum = sum.add(v);
            }
        }
        return sum;
    }
}
