package com.example.securityanalyze.research.application;

import com.example.securityanalyze.common.util.PageUtils;
import com.example.securityanalyze.research.api.AnnualMetricDto;
import com.example.securityanalyze.research.api.CompositeScoreDto;
import com.example.securityanalyze.research.api.DcfRequest;
import com.example.securityanalyze.research.api.DcfResponse;
import com.example.securityanalyze.research.api.FundamentalOverviewResponse;
import com.example.securityanalyze.research.api.FundamentalScreenResponse;
import com.example.securityanalyze.research.api.IndustryPeersResponse;
import com.example.securityanalyze.research.api.IndustryRankItemDto;
import com.example.securityanalyze.research.api.IndustryRankResponse;
import com.example.securityanalyze.research.api.PeerMetricDto;
import com.example.securityanalyze.research.api.ScreenCompanyItemResponse;
import com.example.securityanalyze.research.api.ValuationHistoryItemDto;
import com.example.securityanalyze.research.api.ValuationHistoryResponse;
import com.example.securityanalyze.research.api.ValuationOverviewResponse;
import com.example.securityanalyze.research.api.ValuationWarningDto;
import com.example.securityanalyze.research.domain.AnnualMetric;
import com.example.securityanalyze.research.domain.CompanyBasicInfo;
import com.example.securityanalyze.research.domain.FundamentalMetrics;
import com.example.securityanalyze.research.domain.FundamentalMetricsRepository;
import com.example.securityanalyze.research.domain.IndustryRankItem;
import com.example.securityanalyze.research.domain.MetricStats;
import com.example.securityanalyze.research.domain.PeerMetric;
import com.example.securityanalyze.research.domain.ScreenCompanyItem;
import com.example.securityanalyze.research.domain.StockFundamentalMetrics;
import com.example.securityanalyze.research.domain.StockFundamentalMetricsRepository;
import com.example.securityanalyze.research.domain.ValuationMetrics;
import com.example.securityanalyze.research.domain.ValuationMetricsRepository;
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
    private final ValuationMetricsRepository valuationMetricsRepository;

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

        // 本地排序（复制为可变列表避免 UnsupportedOperationException）
        items = new java.util.ArrayList<>(items);
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

    // ========== 阶段C：估值分析 ==========

    public Optional<ValuationOverviewResponse> getValuationOverview(String stockCode) {
        log.debug("获取估值概览, stockCode={}", stockCode);
        Optional<ValuationMetrics> vmOpt = valuationMetricsRepository.findLatestByStockCode(stockCode);
        if (vmOpt.isEmpty()) {
            return Optional.empty();
        }

        CompanyBasicInfo info = valuationMetricsRepository.findCompanyBasicInfo(stockCode);
        ValuationMetrics vm = vmOpt.get();

        ValuationOverviewResponse response = new ValuationOverviewResponse();
        response.setStockCode(stockCode);
        response.setStockName(info != null ? info.getStockName() : stockCode);
        response.setCurrentPrice(vm.getClosePrice());
        response.setMarketCap(calculateMarketCap(vm.getClosePrice(), info != null ? info.getTotalShares() : null));
        response.setPeTtm(vm.getPeTtm());
        response.setPeTtmPercentile(vm.getPeTtmPercentile());
        response.setPeLyr(vm.getPeLyr());
        response.setPb(vm.getPb());
        response.setPbPercentile(vm.getPbPercentile());
        response.setPsTtm(vm.getPsTtm());
        response.setPsTtmPercentile(vm.getPsTtmPercentile());

        // 综合评分
        response.setCompositeScore(calculateCompositeScore(stockCode, vm));

        // 预警
        response.setWarnings(generateWarnings(vm));

        return Optional.of(response);
    }

    public ValuationHistoryResponse getValuationHistory(String stockCode) {
        log.debug("获取估值历史, stockCode={}", stockCode);
        java.time.LocalDate endDate = java.time.LocalDate.now();
        java.time.LocalDate startDate = endDate.minusYears(5);

        List<ValuationMetrics> history = valuationMetricsRepository.findHistoryByStockCode(stockCode, startDate, endDate);
        CompanyBasicInfo info = valuationMetricsRepository.findCompanyBasicInfo(stockCode);

        ValuationHistoryResponse response = new ValuationHistoryResponse();
        response.setStockCode(stockCode);
        response.setStockName(info != null ? info.getStockName() : stockCode);
        response.setItems(history.stream().map(this::toHistoryItemDto).toList());
        return response;
    }

    public DcfResponse calculateDcf(String stockCode, DcfRequest request) {
        log.debug("DCF估值计算, stockCode={}, request={}", stockCode, request);
        CompanyBasicInfo info = valuationMetricsRepository.findCompanyBasicInfo(stockCode);
        BigDecimal totalShares = info != null ? info.getTotalShares() : null;

        // 获取自由现金流基数
        BigDecimal baseCashFlow = request.getBaseCashFlow();
        if (baseCashFlow == null) {
            baseCashFlow = valuationMetricsRepository.findLatestOperatingCashFlow(stockCode);
        }

        // 获取当前股价（用于计算 upside）
        Optional<ValuationMetrics> vmOpt = valuationMetricsRepository.findLatestByStockCode(stockCode);
        BigDecimal currentPrice = vmOpt.map(ValuationMetrics::getClosePrice).orElse(null);

        // 默认参数
        BigDecimal growthRate = request.getGrowthRate() != null ? request.getGrowthRate() : new BigDecimal("0.10");
        BigDecimal discountRate = request.getDiscountRate() != null ? request.getDiscountRate() : new BigDecimal("0.08");
        BigDecimal terminalGrowthRate = request.getTerminalGrowthRate() != null ? request.getTerminalGrowthRate() : new BigDecimal("0.03");
        int projectionYears = request.getProjectionYears() != null ? request.getProjectionYears() : 10;

        BigDecimal fairPrice = computeDcfFairPrice(baseCashFlow, totalShares, growthRate, discountRate, terminalGrowthRate, projectionYears);

        // 敏感性分析：增长率 ±2%，折现率 ±1%
        BigDecimal fairPriceLow = computeDcfFairPrice(baseCashFlow, totalShares, growthRate.subtract(new BigDecimal("0.02")), discountRate.add(new BigDecimal("0.01")), terminalGrowthRate, projectionYears);
        BigDecimal fairPriceHigh = computeDcfFairPrice(baseCashFlow, totalShares, growthRate.add(new BigDecimal("0.02")), discountRate.subtract(new BigDecimal("0.01")), terminalGrowthRate, projectionYears);

        DcfResponse response = new DcfResponse();
        response.setFairPrice(fairPrice);
        response.setFairPriceRangeLow(fairPriceLow);
        response.setFairPriceRangeHigh(fairPriceHigh);

        if (currentPrice != null && fairPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal upside = safeDivide(fairPrice.subtract(currentPrice), currentPrice, 4);
            response.setUpsidePercent(upside != null ? upside.multiply(BigDecimal.valueOf(100)) : null);
        }

        DcfRequest applied = new DcfRequest();
        applied.setGrowthRate(growthRate);
        applied.setDiscountRate(discountRate);
        applied.setTerminalGrowthRate(terminalGrowthRate);
        applied.setProjectionYears(projectionYears);
        applied.setBaseCashFlow(baseCashFlow);
        response.setAppliedAssumptions(applied);

        return response;
    }

    private BigDecimal calculateMarketCap(BigDecimal price, BigDecimal totalShares) {
        if (price == null || totalShares == null) {
            return null;
        }
        return price.multiply(totalShares);
    }

    private CompositeScoreDto calculateCompositeScore(String stockCode, ValuationMetrics vm) {
        Optional<StockFundamentalMetrics> fmOpt = valuationMetricsRepository.findLatestFundamentalMetrics(stockCode);
        StockFundamentalMetrics fm = fmOpt.orElse(null);

        // 财务健康分（阶段B指标加权）
        int financialHealthScore = calculateFinancialHealthScore(fm);

        // 估值吸引力分（分位倒序）
        int valuationAppealScore = calculateValuationAppealScore(vm);

        // 综合得分
        int overallScore = (financialHealthScore + valuationAppealScore) / 2;

        CompositeScoreDto score = new CompositeScoreDto();
        score.setFinancialHealthScore(financialHealthScore);
        score.setValuationAppealScore(valuationAppealScore);
        score.setOverallScore(overallScore);
        return score;
    }

    private int calculateFinancialHealthScore(StockFundamentalMetrics fm) {
        if (fm == null) {
            return 50; // 默认中等分
        }
        // ROE 30% + 毛利率(无直接字段，用ROE代理) + 资产负债率 20% + 现金流/净利润比 20% + 营收增速 10%
        // 简化：使用已有字段映射到 0-100
        int roeScore = scoreByValue(fm.getRoe(), new BigDecimal("5"), new BigDecimal("25")); // 5%~25% 映射
        int debtScore = scoreByValueReverse(fm.getPeriodExpenseRate(), new BigDecimal("5"), new BigDecimal("30")); // 费用率越低越好
        int cashflowScore = scoreByValue(fm.getCashflowProfitRatio(), new BigDecimal("50"), new BigDecimal("150")); // 50%~150%
        int growthScore = scoreByValue(fm.getRevenueYoy(), new BigDecimal("-10"), new BigDecimal("50")); // -10%~50%

        // 加权：ROE 40% + debt 20% + cashflow 20% + growth 20%
        return (roeScore * 40 + debtScore * 20 + cashflowScore * 20 + growthScore * 20) / 100;
    }

    private int calculateValuationAppealScore(ValuationMetrics vm) {
        if (vm == null) {
            return 50;
        }
        // PE分位倒序 40% + PB分位倒序 30% + PS分位倒序 30%
        BigDecimal peP = vm.getPeTtmPercentile() != null ? vm.getPeTtmPercentile() : BigDecimal.valueOf(0.5);
        BigDecimal pbP = vm.getPbPercentile() != null ? vm.getPbPercentile() : BigDecimal.valueOf(0.5);
        BigDecimal psP = vm.getPsTtmPercentile() != null ? vm.getPsTtmPercentile() : BigDecimal.valueOf(0.5);

        int peScore = (int) ((1 - peP.doubleValue()) * 100);
        int pbScore = (int) ((1 - pbP.doubleValue()) * 100);
        int psScore = (int) ((1 - psP.doubleValue()) * 100);

        return (peScore * 40 + pbScore * 30 + psScore * 30) / 100;
    }

    /**
     * 将数值线性映射到 0-100 分数（值越大分数越高）
     */
    private int scoreByValue(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value == null) return 50;
        if (value.compareTo(min) <= 0) return 0;
        if (value.compareTo(max) >= 0) return 100;
        BigDecimal range = max.subtract(min);
        if (range.compareTo(BigDecimal.ZERO) == 0) return 50;
        return value.subtract(min).divide(range, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).intValue();
    }

    /**
     * 将数值线性映射到 0-100 分数（值越小分数越高）
     */
    private int scoreByValueReverse(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value == null) return 50;
        if (value.compareTo(min) <= 0) return 100;
        if (value.compareTo(max) >= 0) return 0;
        BigDecimal range = max.subtract(min);
        if (range.compareTo(BigDecimal.ZERO) == 0) return 50;
        return BigDecimal.ONE.subtract(value.subtract(min).divide(range, 4, RoundingMode.HALF_UP)).multiply(BigDecimal.valueOf(100)).intValue();
    }

    private List<ValuationWarningDto> generateWarnings(ValuationMetrics vm) {
        java.util.List<ValuationWarningDto> warnings = new java.util.ArrayList<>();
        if (vm.getPeTtmPercentile() != null) {
            double p = vm.getPeTtmPercentile().doubleValue();
            if (p > 0.90) {
                warnings.add(createWarning("PE_TTM", "high",
                        String.format("PE(TTM)处于历史%.0f%%分位，估值偏高", p * 100)));
            } else if (p > 0.70) {
                warnings.add(createWarning("PE_TTM", "medium",
                        String.format("PE(TTM)处于历史%.0f%%分位，估值偏贵", p * 100)));
            }
        }
        if (vm.getPbPercentile() != null) {
            double p = vm.getPbPercentile().doubleValue();
            if (p > 0.90) {
                warnings.add(createWarning("PB", "high",
                        String.format("PB处于历史%.0f%%分位，估值偏高", p * 100)));
            }
        }
        return warnings;
    }

    private ValuationWarningDto createWarning(String metric, String level, String message) {
        ValuationWarningDto w = new ValuationWarningDto();
        w.setMetric(metric);
        w.setLevel(level);
        w.setMessage(message);
        return w;
    }

    private ValuationHistoryItemDto toHistoryItemDto(ValuationMetrics vm) {
        ValuationHistoryItemDto dto = new ValuationHistoryItemDto();
        dto.setTradeDate(vm.getTradeDate() != null ? vm.getTradeDate().format(DATE_FORMATTER) : null);
        dto.setClosePrice(vm.getClosePrice());
        dto.setPeTtm(vm.getPeTtm());
        dto.setPeLyr(vm.getPeLyr());
        dto.setPb(vm.getPb());
        dto.setPsTtm(vm.getPsTtm());
        return dto;
    }

    private BigDecimal computeDcfFairPrice(BigDecimal baseCashFlow, BigDecimal totalShares,
                                            BigDecimal growthRate, BigDecimal discountRate,
                                            BigDecimal terminalGrowthRate, int years) {
        if (baseCashFlow == null || totalShares == null ||
            growthRate == null || discountRate == null || terminalGrowthRate == null) {
            return null;
        }
        if (totalShares.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        double cf = baseCashFlow.doubleValue();
        double r = discountRate.doubleValue();
        double g = growthRate.doubleValue();
        double tg = terminalGrowthRate.doubleValue();
        double shares = totalShares.doubleValue();

        if (r <= tg) {
            return null;
        }

        double pv = 0.0;
        for (int t = 1; t <= years; t++) {
            double cfT = cf * Math.pow(1 + g, t);
            pv += cfT / Math.pow(1 + r, t);
        }

        double cfN = cf * Math.pow(1 + g, years);
        double terminalValue = cfN * (1 + tg) / (r - tg);
        pv += terminalValue / Math.pow(1 + r, years);

        return BigDecimal.valueOf(pv / shares).setScale(4, RoundingMode.HALF_UP);
    }
}
