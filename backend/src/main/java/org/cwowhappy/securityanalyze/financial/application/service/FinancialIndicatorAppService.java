package org.cwowhappy.securityanalyze.financial.application.service;

import lombok.RequiredArgsConstructor;
import org.cwowhappy.securityanalyze.financial.application.dto.FinancialIndicatorDTO;
import org.cwowhappy.securityanalyze.financial.domain.model.FinancialIndicator;
import org.cwowhappy.securityanalyze.financial.domain.repository.FinancialIndicatorRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 财务指标应用服务。
 */
@Service
@RequiredArgsConstructor
public class FinancialIndicatorAppService {

    private final FinancialIndicatorRepository indicatorRepository;

    public List<FinancialIndicatorDTO> getIndicators(String stockCode, String reportType, int limit) {
        List<FinancialIndicator> indicators = indicatorRepository.findByStockCode(stockCode, reportType, limit);
        return indicators.stream().map(this::toDTO).toList();
    }

    private FinancialIndicatorDTO toDTO(FinancialIndicator indicator) {
        return FinancialIndicatorDTO.builder()
            .stockCode(indicator.getStockCode())
            .reportDate(indicator.getReportDate())
            .reportType(indicator.getReportType())
            .roe(indicator.getRoe())
            .roa(indicator.getRoa())
            .roic(indicator.getRoic())
            .grossMargin(indicator.getGrossMargin())
            .netMargin(indicator.getNetMargin())
            .netMarginExcl(indicator.getNetMarginExcl())
            .debtRatio(indicator.getDebtRatio())
            .currentRatio(indicator.getCurrentRatio())
            .quickRatio(indicator.getQuickRatio())
            .netDebtRatio(indicator.getNetDebtRatio())
            .equityRatio(indicator.getEquityRatio())
            .dso(indicator.getDso())
            .dio(indicator.getDio())
            .dpo(indicator.getDpo())
            .ccc(indicator.getCcc())
            .assetTurnover(indicator.getAssetTurnover())
            .fixedAssetTurnover(indicator.getFixedAssetTurnover())
            .revenueGrowth(indicator.getRevenueGrowth())
            .npParentGrowth(indicator.getNpParentGrowth())
            .npExclGrowth(indicator.getNpExclGrowth())
            .cfoGrowth(indicator.getCfoGrowth())
            .equityGrowth(indicator.getEquityGrowth())
            .assetGrowth(indicator.getAssetGrowth())
            .pe(indicator.getPe())
            .pb(indicator.getPb())
            .ps(indicator.getPs())
            .peg(indicator.getPeg())
            .evEbitda(indicator.getEvEbitda())
            .dividendYield(indicator.getDividendYield())
            .marketCap(indicator.getMarketCap())
            .cfoToNp(indicator.getCfoToNp())
            .build();
    }
}
