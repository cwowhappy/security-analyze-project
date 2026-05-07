package com.example.securityanalyze.portfolio.application;

import com.example.securityanalyze.portfolio.domain.Position;
import com.example.securityanalyze.portfolio.domain.TradeType;
import com.example.securityanalyze.portfolio.domain.TransactionRecord;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class PositionCalculationService {

    public Position calculate(Long portfolioId, String stockCode, List<TransactionRecord> transactions) {
        Position position = new Position();
        position.setPortfolioId(portfolioId);
        position.setStockCode(stockCode);
        position.setCurrentQuantity(BigDecimal.ZERO);
        position.setTotalCost(BigDecimal.ZERO);
        position.setAvgCost(BigDecimal.ZERO);
        position.setRealizedPnl(BigDecimal.ZERO);

        if (transactions == null || transactions.isEmpty()) {
            return position;
        }

        for (TransactionRecord tx : transactions) {
            BigDecimal qty = tx.getQuantity() != null ? tx.getQuantity() : BigDecimal.ZERO;
            BigDecimal price = tx.getPrice() != null ? tx.getPrice() : BigDecimal.ZERO;
            BigDecimal fee = tx.getFee() != null ? tx.getFee() : BigDecimal.ZERO;
            BigDecimal tax = tx.getTax() != null ? tx.getTax() : BigDecimal.ZERO;

            switch (tx.getTradeType()) {
                case BUY, RIGHTS -> {
                    position.setCurrentQuantity(position.getCurrentQuantity().add(qty));
                    // 买入成本包含成交价和交易费用
                    position.setTotalCost(position.getTotalCost().add(price.multiply(qty)).add(fee).add(tax));
                    position.setAvgCost(position.getTotalCost()
                            .divide(position.getCurrentQuantity(), 4, RoundingMode.HALF_UP));
                    if (position.getFirstBuyDate() == null) {
                        position.setFirstBuyDate(tx.getTradeDate());
                    }
                }
                case SELL -> {
                    if (position.getCurrentQuantity().compareTo(qty) < 0) {
                        throw new IllegalStateException("卖出股数超过当前持仓数量");
                    }
                    BigDecimal sellCost = position.getAvgCost().multiply(qty);
                    BigDecimal sellProceeds = price.multiply(qty);
                    BigDecimal pnl = sellProceeds.subtract(sellCost).subtract(fee).subtract(tax);
                    position.setRealizedPnl(position.getRealizedPnl().add(pnl));
                    position.setCurrentQuantity(position.getCurrentQuantity().subtract(qty));
                    position.setTotalCost(position.getTotalCost().subtract(sellCost));
                }
                case BONUS, SPLIT -> {
                    position.setCurrentQuantity(position.getCurrentQuantity().add(qty));
                    if (position.getCurrentQuantity().compareTo(BigDecimal.ZERO) > 0) {
                        position.setAvgCost(position.getTotalCost()
                                .divide(position.getCurrentQuantity(), 4, RoundingMode.HALF_UP));
                    }
                }
                case DIVIDEND -> {
                    // 现金分红不影响持仓数量和成本
                }
                default -> {
                    // OTHER, MERGER 等暂不处理持仓变动
                }
            }

            position.setLastTradeDate(tx.getTradeDate());
        }

        if (position.getCurrentQuantity().compareTo(BigDecimal.ZERO) > 0) {
            position.setAvgCost(position.getTotalCost()
                    .divide(position.getCurrentQuantity(), 4, RoundingMode.HALF_UP));
        } else {
            position.setCurrentQuantity(BigDecimal.ZERO);
            position.setTotalCost(BigDecimal.ZERO);
            position.setAvgCost(BigDecimal.ZERO);
        }

        return position;
    }
}
