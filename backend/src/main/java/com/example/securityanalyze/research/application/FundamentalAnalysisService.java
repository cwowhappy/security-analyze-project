package com.example.securityanalyze.research.application;

import com.example.securityanalyze.common.util.PageUtils;
import com.example.securityanalyze.research.api.AnnualMetricDto;
import com.example.securityanalyze.research.api.FundamentalOverviewResponse;
import com.example.securityanalyze.research.api.FundamentalScreenResponse;
import com.example.securityanalyze.research.api.IndustryPeersResponse;
import com.example.securityanalyze.research.api.IndustryRankItemDto;
import com.example.securityanalyze.research.api.IndustryRankResponse;
import com.example.securityanalyze.research.api.PeerMetricDto;
import com.example.securityanalyze.research.api.ScreenCompanyItemResponse;
import com.example.securityanalyze.research.domain.AnnualMetric;
import com.example.securityanalyze.research.domain.FundamentalMetrics;
import com.example.securityanalyze.research.domain.FundamentalMetricsRepository;
import com.example.securityanalyze.research.domain.IndustryRankItem;
import com.example.securityanalyze.research.domain.PeerMetric;
import com.example.securityanalyze.research.domain.ScreenCompanyItem;
import com.example.securityanalyze.research.domain.StockFundamentalMetrics;
import com.example.securityanalyze.research.domain.StockFundamentalMetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FundamentalAnalysisService {

    private final FundamentalMetricsRepository fundamentalMetricsRepository;
    private final StockFundamentalMetricsRepository stockFundamentalMetricsRepository;

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

    public IndustryRankResponse getIndustryRank(String stockCode, String sortBy, String order) {
        log.debug("获取行业排名, stockCode={}, sortBy={}, order={}", stockCode, sortBy, order);

        String industry = fundamentalMetricsRepository.findIndustryByStockCode(stockCode);

        if (industry == null || industry.isBlank()) {
            log.warn("行业排名查询失败, 股票不存在或行业为空: stockCode={}", stockCode);
            IndustryRankResponse empty = new IndustryRankResponse();
            empty.setRank(0);
            empty.setTotal(0);
            empty.setSortBy(sortBy);
            empty.setOrder(order);
            empty.setItems(List.of());
            return empty;
        }

        List<IndustryRankItem> items = fundamentalMetricsRepository.findIndustryRankItems(industry);

        // 本地排序
        items.sort((a, b) -> compareForRank(a, b, sortBy, order));

        // 计算目标公司排名
        int rank = 0;
        for (int i = 0; i < items.size(); i++) {
            if (stockCode.equals(items.get(i).getStockCode())) {
                rank = i + 1;
                break;
            }
        }

        IndustryRankResponse response = new IndustryRankResponse();
        response.setRank(rank);
        response.setTotal(items.size());
        response.setSortBy(sortBy);
        response.setOrder(order);
        response.setItems(items.stream().map(this::toRankDto).toList());
        return response;
    }

    private int compareForRank(IndustryRankItem a, IndustryRankItem b, String sortBy, String order) {
        BigDecimal va = extractValue(a, sortBy);
        BigDecimal vb = extractValue(b, sortBy);
        int dir = "asc".equalsIgnoreCase(order) ? 1 : -1;

        if (va == null && vb == null) return 0;
        if (va == null) return 1 * dir;
        if (vb == null) return -1 * dir;
        return va.compareTo(vb) * dir;
    }

    private BigDecimal extractValue(IndustryRankItem item, String sortBy) {
        return switch (sortBy.toLowerCase()) {
            case "revenue" -> item.getTotalRevenue();
            case "profit" -> item.getParentNetProfit();
            case "grossmargin" -> item.getGrossMargin();
            case "roe" -> item.getRoe();
            case "debtratio" -> item.getDebtRatio();
            default -> item.getRoe();
        };
    }

    private IndustryRankItemDto toRankDto(IndustryRankItem item) {
        IndustryRankItemDto dto = new IndustryRankItemDto();
        dto.setStockCode(item.getStockCode());
        dto.setStockName(item.getStockName());
        dto.setIndustry(item.getIndustry());
        dto.setTotalRevenue(item.getTotalRevenue());
        dto.setParentNetProfit(item.getParentNetProfit());
        dto.setGrossMargin(item.getGrossMargin());
        dto.setRoe(item.getRoe());
        dto.setDebtRatio(item.getDebtRatio());
        return dto;
    }

    private FundamentalOverviewResponse toOverviewResponse(FundamentalMetrics metrics) {
        FundamentalOverviewResponse response = new FundamentalOverviewResponse();
        response.setStockCode(metrics.getStockCode());
        response.setStockName(metrics.getStockName());
        response.setIndustry(metrics.getIndustry());
        response.setMarket(metrics.getMarket());

        // 批量预加载预计算指标，避免 N+1
        Map<Integer, StockFundamentalMetrics> precomputedMap = loadPrecomputedMetrics(metrics.getStockCode());

        response.setMetrics(metrics.getAnnualMetrics().stream()
                .map(m -> this.toAnnualMetricDto(m, precomputedMap))
                .collect(Collectors.toList()));
        return response;
    }

    private Map<Integer, StockFundamentalMetrics> loadPrecomputedMetrics(String stockCode) {
        try {
            return stockFundamentalMetricsRepository.findByStockCode(stockCode, 5).stream()
                    .collect(Collectors.toMap(StockFundamentalMetrics::getReportYear, pm -> pm));
        } catch (org.springframework.dao.DataAccessException e) {
            log.warn("批量加载预计算指标失败, stockCode={}, fallback到实时计算", stockCode, e);
            return Map.of();
        }
    }

    private AnnualMetricDto toAnnualMetricDto(AnnualMetric metric, Map<Integer, StockFundamentalMetrics> precomputedMap) {
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
        dto.setTotalCurrentAssets(metric.getTotalCurrentAssets());
        dto.setTotalNoncurrentAssets(metric.getTotalNoncurrentAssets());
        dto.setTotalCurrentLiabilities(metric.getTotalCurrentLiabilities());
        dto.setTotalNoncurrentLiabilities(metric.getTotalNoncurrentLiabilities());
        dto.setOperatingCashFlow(metric.getOperatingCashFlow());
        dto.setInvestingCashFlow(metric.getInvestingCashFlow());
        dto.setFinancingCashFlow(metric.getFinancingCashFlow());
        dto.setEndCce(metric.getEndCce());
        dto.setSaleExpense(metric.getSaleExpense());
        dto.setManageExpense(metric.getManageExpense());
        dto.setResearchExpense(metric.getResearchExpense());
        dto.setFinanceExpense(metric.getFinanceExpense());

        // 计算衍生指标（实时计算兜底）
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

        // 阶段B：从批量预加载的 Map 中读取衍生指标（优先使用）
        mergePrecomputedMetrics(dto, precomputedMap.get(metric.getReportYear()));

        return dto;
    }

    private void mergePrecomputedMetrics(AnnualMetricDto dto, StockFundamentalMetrics pm) {
        if (pm == null) {
            return;
        }
        if (pm.getRevenueYoy() != null) dto.setRevenueYoy(pm.getRevenueYoy());
        if (pm.getProfitYoy() != null) dto.setProfitYoy(pm.getProfitYoy());
        if (pm.getRoa() != null) dto.setRoa(pm.getRoa());
        if (pm.getAssetTurnover() != null) dto.setAssetTurnover(pm.getAssetTurnover());
        if (pm.getEquityMultiplier() != null) dto.setEquityMultiplier(pm.getEquityMultiplier());
        if (pm.getCurrentRatio() != null) dto.setCurrentRatio(pm.getCurrentRatio());
        if (pm.getQuickRatio() != null) dto.setQuickRatio(pm.getQuickRatio());
        // ROE 也用预计算值覆盖（确保与杜邦分析一致）
        if (pm.getRoe() != null) dto.setRoe(pm.getRoe());
        // 期间费用率和现金流利润比也覆盖
        if (pm.getPeriodExpenseRate() != null) dto.setPeriodExpenseRate(pm.getPeriodExpenseRate());
        if (pm.getCashflowProfitRatio() != null) dto.setCashflowProfitRatio(pm.getCashflowProfitRatio());
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
