package com.example.securityanalyze.portfolio.api;

import com.example.securityanalyze.portfolio.application.PortfolioService;
import com.example.securityanalyze.portfolio.application.TransactionService;
import com.example.securityanalyze.portfolio.domain.Portfolio;
import com.example.securityanalyze.portfolio.domain.Position;
import com.example.securityanalyze.portfolio.domain.TransactionRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/portfolios")
@RequiredArgsConstructor
public class PositionController {

    private final PortfolioService portfolioService;
    private final TransactionService transactionService;

    @GetMapping("/{portfolioId}/positions")
    public ResponseEntity<List<PositionResponse>> listPositions(@PathVariable Long portfolioId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        portfolioService.getPortfolio(username, portfolioId);
        List<Map<String, Object>> rows = portfolioService.listPositionsWithQuote(portfolioId);

        BigDecimal totalMarketValue = BigDecimal.ZERO;
        List<PositionResponse> responses = rows.stream().map(this::toResponse).toList();
        for (PositionResponse r : responses) {
            if (r.getMarketValue() != null) {
                totalMarketValue = totalMarketValue.add(r.getMarketValue());
            }
        }

        // 计算权重
        if (totalMarketValue.compareTo(BigDecimal.ZERO) > 0) {
            for (PositionResponse r : responses) {
                if (r.getMarketValue() != null) {
                    r.setWeight(r.getMarketValue()
                            .multiply(new BigDecimal("100"))
                            .divide(totalMarketValue, 2, RoundingMode.HALF_UP));
                }
            }
        }

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{portfolioId}/summary")
    public ResponseEntity<PortfolioSummaryResponse> getSummary(@PathVariable Long portfolioId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Portfolio portfolio = portfolioService.getPortfolio(username, portfolioId);
        List<Map<String, Object>> rows = portfolioService.listPositionsWithQuote(portfolioId);
        List<TransactionRecord> allTx = transactionService.listTransactions(portfolioId, null, null, null, null, 0, Integer.MAX_VALUE);

        PortfolioSummaryResponse summary = new PortfolioSummaryResponse();
        summary.setPortfolioId(portfolioId);
        summary.setPortfolioName(portfolio.getName());

        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalMarketValue = BigDecimal.ZERO;
        BigDecimal totalRealizedPnl = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            BigDecimal cost = toBigDecimal(row.get("total_cost"));
            BigDecimal marketValue = calculateMarketValue(row);
            BigDecimal realizedPnl = toBigDecimal(row.get("realized_pnl"));
            totalCost = totalCost.add(cost);
            totalMarketValue = totalMarketValue.add(marketValue);
            totalRealizedPnl = totalRealizedPnl.add(realizedPnl);
        }

        BigDecimal totalFloatingPnl = totalMarketValue.subtract(totalCost);
        BigDecimal totalAssetReturn = totalFloatingPnl.add(totalRealizedPnl);

        summary.setTotalCost(totalCost);
        summary.setTotalMarketValue(totalMarketValue);
        summary.setTotalFloatingPnl(totalFloatingPnl);
        summary.setTotalRealizedPnl(totalRealizedPnl);
        summary.setTotalAssetReturn(totalAssetReturn);

        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            summary.setTotalFloatingPnlRate(totalFloatingPnl
                    .multiply(new BigDecimal("100"))
                    .divide(totalCost, 2, RoundingMode.HALF_UP));
            summary.setTotalAssetReturnRate(totalAssetReturn
                    .multiply(new BigDecimal("100"))
                    .divide(totalCost, 2, RoundingMode.HALF_UP));
        } else {
            summary.setTotalFloatingPnlRate(BigDecimal.ZERO);
            summary.setTotalAssetReturnRate(BigDecimal.ZERO);
        }

        summary.setHoldingCount(rows.size());
        summary.setLatestTradeDate(allTx.isEmpty() ? null : allTx.get(0).getTradeDate());

        return ResponseEntity.ok(summary);
    }

    private PositionResponse toResponse(Map<String, Object> row) {
        PositionResponse response = new PositionResponse();
        response.setStockCode((String) row.get("stock_code"));
        response.setStockName((String) row.get("stock_name"));
        response.setIndustry((String) row.get("industry"));
        response.setMarket((String) row.get("market"));
        response.setCurrentQuantity(toBigDecimal(row.get("current_quantity")));
        response.setAvgCost(toBigDecimal(row.get("avg_cost")));
        response.setTotalCost(toBigDecimal(row.get("total_cost")));
        response.setRealizedPnl(toBigDecimal(row.get("realized_pnl")));
        Object firstBuyDate = row.get("first_buy_date");
        if (firstBuyDate instanceof java.sql.Date) {
            response.setFirstBuyDate(((java.sql.Date) firstBuyDate).toLocalDate());
        } else if (firstBuyDate instanceof java.time.LocalDate) {
            response.setFirstBuyDate((java.time.LocalDate) firstBuyDate);
        }
        Object lastTradeDate = row.get("last_trade_date");
        if (lastTradeDate instanceof java.sql.Date) {
            response.setLastTradeDate(((java.sql.Date) lastTradeDate).toLocalDate());
        } else if (lastTradeDate instanceof java.time.LocalDate) {
            response.setLastTradeDate((java.time.LocalDate) lastTradeDate);
        }

        BigDecimal closePrice = toBigDecimal(row.get("close_price"));
        response.setClosePrice(closePrice);

        BigDecimal currentQty = response.getCurrentQuantity();
        if (closePrice != null && currentQty != null) {
            BigDecimal marketValue = closePrice.multiply(currentQty).setScale(2, RoundingMode.HALF_UP);
            response.setMarketValue(marketValue);

            BigDecimal avgCost = response.getAvgCost();
            if (avgCost != null && avgCost.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal floatingPnl = marketValue.subtract(avgCost.multiply(currentQty));
                response.setFloatingPnl(floatingPnl.setScale(2, RoundingMode.HALF_UP));
                response.setFloatingPnlRate(
                        closePrice.subtract(avgCost)
                                .multiply(new BigDecimal("100"))
                                .divide(avgCost, 2, RoundingMode.HALF_UP)
                );
            }
        }

        return response;
    }

    private BigDecimal calculateMarketValue(Map<String, Object> row) {
        BigDecimal closePrice = toBigDecimal(row.get("close_price"));
        BigDecimal currentQty = toBigDecimal(row.get("current_quantity"));
        if (closePrice != null && currentQty != null) {
            return closePrice.multiply(currentQty).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return null;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        if (val instanceof Number) return BigDecimal.valueOf(((Number) val).doubleValue());
        try {
            return new BigDecimal(val.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
