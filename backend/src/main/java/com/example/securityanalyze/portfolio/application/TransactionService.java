package com.example.securityanalyze.portfolio.application;

import com.example.securityanalyze.portfolio.api.InsufficientPositionException;
import com.example.securityanalyze.portfolio.api.PortfolioAccessDeniedException;
import com.example.securityanalyze.portfolio.api.PortfolioNotFoundException;
import com.example.securityanalyze.portfolio.api.TransactionNotFoundException;
import com.example.securityanalyze.portfolio.domain.*;
import com.example.securityanalyze.user.domain.User;
import com.example.securityanalyze.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final PositionRepository positionRepository;
    private final PortfolioRepository portfolioRepository;
    private final PositionCalculationService calculationService;
    private final UserRepository userRepository;

    @Transactional
    public TransactionRecord createTransaction(String username, Long portfolioId, String stockCode,
                                                LocalDate tradeDate, TradeType tradeType,
                                                BigDecimal price, BigDecimal quantity,
                                                BigDecimal fee, BigDecimal tax, String remark) {
        User user = getUser(username);
        validatePortfolioAccess(user.getId(), portfolioId);

        TransactionRecord tx = new TransactionRecord();
        tx.setPortfolioId(portfolioId);
        tx.setStockCode(stockCode);
        tx.setTradeDate(tradeDate);
        tx.setTradeType(tradeType);
        tx.setPrice(price);
        tx.setQuantity(quantity);
        tx.setFee(fee != null ? fee : BigDecimal.ZERO);
        tx.setTax(tax != null ? tax : BigDecimal.ZERO);
        if (price != null && quantity != null) {
            tx.setAmount(price.multiply(quantity));
        }
        tx.setRemark(remark);

        TransactionRecord saved = transactionRepository.save(tx);
        recalculatePosition(portfolioId, stockCode);
        return saved;
    }

    @Transactional
    public TransactionRecord updateTransaction(String username, Long transactionId, String stockCode,
                                                LocalDate tradeDate, TradeType tradeType,
                                                BigDecimal price, BigDecimal quantity,
                                                BigDecimal fee, BigDecimal tax, String remark) {
        User user = getUser(username);
        TransactionRecord existing = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("成交记录不存在"));
        validatePortfolioAccess(user.getId(), existing.getPortfolioId());

        existing.setStockCode(stockCode);
        existing.setTradeDate(tradeDate);
        existing.setTradeType(tradeType);
        existing.setPrice(price);
        existing.setQuantity(quantity);
        existing.setFee(fee != null ? fee : BigDecimal.ZERO);
        existing.setTax(tax != null ? tax : BigDecimal.ZERO);
        if (price != null && quantity != null) {
            existing.setAmount(price.multiply(quantity));
        } else {
            existing.setAmount(null);
        }
        existing.setRemark(remark);

        transactionRepository.update(existing);
        recalculatePosition(existing.getPortfolioId(), stockCode);
        return existing;
    }

    @Transactional
    public void deleteTransaction(String username, Long transactionId) {
        User user = getUser(username);
        TransactionRecord existing = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("成交记录不存在"));
        validatePortfolioAccess(user.getId(), existing.getPortfolioId());

        transactionRepository.softDelete(transactionId);
        recalculatePosition(existing.getPortfolioId(), existing.getStockCode());
    }

    @Transactional(readOnly = true)
    public List<TransactionRecord> listTransactions(Long portfolioId, String stockCode, TradeType tradeType,
                                                     String startDate, String endDate, int offset, int limit) {
        return transactionRepository.findByPortfolioId(portfolioId, stockCode, tradeType, startDate, endDate, offset, limit);
    }

    @Transactional(readOnly = true)
    public long countTransactions(Long portfolioId, String stockCode, TradeType tradeType,
                                  String startDate, String endDate) {
        return transactionRepository.countByPortfolioId(portfolioId, stockCode, tradeType, startDate, endDate);
    }

    private void recalculatePosition(Long portfolioId, String stockCode) {
        List<TransactionRecord> activeTxs = transactionRepository.findActiveByPortfolioIdAndStockCode(portfolioId, stockCode);
        Position calculated = calculationService.calculate(portfolioId, stockCode, activeTxs);

        BigDecimal currentQty = calculated.getCurrentQuantity() != null ? calculated.getCurrentQuantity() : BigDecimal.ZERO;
        if (currentQty.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientPositionException("卖出股数超过当前可卖持仓");
        }

        Position existing = positionRepository.findByPortfolioIdAndStockCode(portfolioId, stockCode).orElse(null);
        BigDecimal realizedPnl = calculated.getRealizedPnl() != null ? calculated.getRealizedPnl() : BigDecimal.ZERO;

        if (existing != null) {
            if (currentQty.compareTo(BigDecimal.ZERO) == 0 && realizedPnl.compareTo(BigDecimal.ZERO) == 0) {
                // 持仓已清空且无已实现盈亏，删除该持仓记录
                positionRepository.softDelete(existing.getId());
            } else {
                existing.setCurrentQuantity(currentQty);
                existing.setTotalCost(calculated.getTotalCost() != null ? calculated.getTotalCost() : BigDecimal.ZERO);
                existing.setAvgCost(calculated.getAvgCost() != null ? calculated.getAvgCost() : BigDecimal.ZERO);
                existing.setRealizedPnl(realizedPnl);
                existing.setFirstBuyDate(calculated.getFirstBuyDate());
                existing.setLastTradeDate(calculated.getLastTradeDate());
                positionRepository.update(existing);
            }
        } else {
            if (currentQty.compareTo(BigDecimal.ZERO) > 0 || realizedPnl.compareTo(BigDecimal.ZERO) > 0) {
                positionRepository.save(calculated);
            }
        }
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new PortfolioAccessDeniedException("用户不存在"));
    }

    private void validatePortfolioAccess(Long userId, Long portfolioId) {
        portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new PortfolioNotFoundException("组合不存在或无权访问"));
    }
}
