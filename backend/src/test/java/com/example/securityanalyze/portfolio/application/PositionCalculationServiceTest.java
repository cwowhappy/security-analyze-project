package com.example.securityanalyze.portfolio.application;

import com.example.securityanalyze.portfolio.domain.Position;
import com.example.securityanalyze.portfolio.domain.TradeType;
import com.example.securityanalyze.portfolio.domain.TransactionRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PositionCalculationServiceTest {

    private final PositionCalculationService service = new PositionCalculationService();

    private TransactionRecord tx(TradeType type, BigDecimal price, BigDecimal quantity,
                                  BigDecimal fee, BigDecimal tax) {
        TransactionRecord t = new TransactionRecord();
        t.setTradeType(type);
        t.setPrice(price);
        t.setQuantity(quantity);
        t.setFee(fee != null ? fee : BigDecimal.ZERO);
        t.setTax(tax != null ? tax : BigDecimal.ZERO);
        t.setTradeDate(LocalDate.now());
        return t;
    }

    @Test
    void shouldCalculateBuyPosition() {
        List<TransactionRecord> txs = List.of(
                tx(TradeType.BUY, new BigDecimal("100"), new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO)
        );

        Position pos = service.calculate(1L, "600519", txs);

        assertEquals(0, new BigDecimal("10").compareTo(pos.getCurrentQuantity()));
        assertEquals(0, new BigDecimal("1000").compareTo(pos.getTotalCost()));
        assertEquals(0, new BigDecimal("100").compareTo(pos.getAvgCost()));
        assertEquals(BigDecimal.ZERO, pos.getRealizedPnl());
    }

    @Test
    void shouldIncludeFeeAndTaxInBuyCost() {
        List<TransactionRecord> txs = List.of(
                tx(TradeType.BUY, new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("5"), new BigDecimal("1"))
        );

        Position pos = service.calculate(1L, "600519", txs);

        // total_cost = 100*10 + 5 + 1 = 1006
        // avg_cost = 1006 / 10 = 100.6
        assertEquals(0, new BigDecimal("10").compareTo(pos.getCurrentQuantity()));
        assertEquals(0, new BigDecimal("1006").compareTo(pos.getTotalCost()));
        assertEquals(0, new BigDecimal("100.6").compareTo(pos.getAvgCost()));
    }

    @Test
    void shouldCalculateMultipleBuys() {
        List<TransactionRecord> txs = List.of(
                tx(TradeType.BUY, new BigDecimal("100"), new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO),
                tx(TradeType.BUY, new BigDecimal("120"), new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO)
        );

        Position pos = service.calculate(1L, "600519", txs);

        assertEquals(0, new BigDecimal("20").compareTo(pos.getCurrentQuantity()));
        assertEquals(0, new BigDecimal("2200").compareTo(pos.getTotalCost()));
        assertEquals(0, new BigDecimal("110").compareTo(pos.getAvgCost()));
    }

    @Test
    void shouldCalculateSellWithRealizedPnl() {
        List<TransactionRecord> txs = List.of(
                tx(TradeType.BUY, new BigDecimal("100"), new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO),
                tx(TradeType.SELL, new BigDecimal("120"), new BigDecimal("5"), new BigDecimal("5"), new BigDecimal("1"))
        );

        Position pos = service.calculate(1L, "600519", txs);

        // sell_cost = 100 * 5 = 500
        // realized_pnl = (120 * 5) - 500 - 5 - 1 = 600 - 506 = 94
        assertEquals(0, new BigDecimal("5").compareTo(pos.getCurrentQuantity()));
        assertEquals(0, new BigDecimal("500").compareTo(pos.getTotalCost()));
        assertEquals(0, new BigDecimal("100").compareTo(pos.getAvgCost()));
        assertEquals(0, new BigDecimal("94").compareTo(pos.getRealizedPnl()));
    }

    @Test
    void shouldClearCostWhenLiquidated() {
        List<TransactionRecord> txs = List.of(
                tx(TradeType.BUY, new BigDecimal("100"), new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO),
                tx(TradeType.SELL, new BigDecimal("120"), new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO)
        );

        Position pos = service.calculate(1L, "600519", txs);

        assertEquals(BigDecimal.ZERO, pos.getCurrentQuantity());
        assertEquals(BigDecimal.ZERO, pos.getTotalCost());
        assertEquals(BigDecimal.ZERO, pos.getAvgCost());
        assertEquals(0, new BigDecimal("200").compareTo(pos.getRealizedPnl()));
    }

    @Test
    void shouldHandleDividendWithoutChangingPosition() {
        List<TransactionRecord> txs = List.of(
                tx(TradeType.BUY, new BigDecimal("100"), new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO),
                tx(TradeType.DIVIDEND, null, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("10"))
        );

        Position pos = service.calculate(1L, "600519", txs);

        assertEquals(0, new BigDecimal("10").compareTo(pos.getCurrentQuantity()));
        assertEquals(0, new BigDecimal("1000").compareTo(pos.getTotalCost()));
        assertEquals(0, new BigDecimal("100").compareTo(pos.getAvgCost()));
    }

    @Test
    void shouldHandleBonusIssue() {
        List<TransactionRecord> txs = List.of(
                tx(TradeType.BUY, new BigDecimal("100"), new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO),
                tx(TradeType.BONUS, null, new BigDecimal("5"), BigDecimal.ZERO, BigDecimal.ZERO)
        );

        Position pos = service.calculate(1L, "600519", txs);

        assertEquals(new BigDecimal("15"), pos.getCurrentQuantity());
        assertEquals(new BigDecimal("1000"), pos.getTotalCost());
        // avg_cost = 1000 / 15 = 66.6667
        assertEquals(0, new BigDecimal("66.6667").compareTo(pos.getAvgCost()));
    }

    @Test
    void shouldHandleRightsIssue() {
        List<TransactionRecord> txs = List.of(
                tx(TradeType.BUY, new BigDecimal("100"), new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO),
                tx(TradeType.RIGHTS, new BigDecimal("80"), new BigDecimal("5"), BigDecimal.ZERO, BigDecimal.ZERO)
        );

        Position pos = service.calculate(1L, "600519", txs);

        assertEquals(new BigDecimal("15"), pos.getCurrentQuantity());
        assertEquals(new BigDecimal("1400"), pos.getTotalCost());
        // avg_cost = 1400 / 15 = 93.3333
        assertEquals(0, new BigDecimal("93.3333").compareTo(pos.getAvgCost()));
    }

    @Test
    void shouldReturnEmptyPositionWhenNoTransactions() {
        Position pos = service.calculate(1L, "600519", List.of());

        assertEquals(BigDecimal.ZERO, pos.getCurrentQuantity());
        assertEquals(BigDecimal.ZERO, pos.getTotalCost());
        assertEquals(BigDecimal.ZERO, pos.getAvgCost());
        assertEquals(BigDecimal.ZERO, pos.getRealizedPnl());
    }

    @Test
    void shouldSetFirstBuyDateAndLastTradeDate() {
        TransactionRecord t1 = tx(TradeType.BUY, new BigDecimal("100"), new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO);
        t1.setTradeDate(LocalDate.of(2026, 1, 1));
        TransactionRecord t2 = tx(TradeType.BUY, new BigDecimal("110"), new BigDecimal("5"), BigDecimal.ZERO, BigDecimal.ZERO);
        t2.setTradeDate(LocalDate.of(2026, 3, 1));

        Position pos = service.calculate(1L, "600519", List.of(t1, t2));

        assertEquals(LocalDate.of(2026, 1, 1), pos.getFirstBuyDate());
        assertEquals(LocalDate.of(2026, 3, 1), pos.getLastTradeDate());
    }
}
