package com.example.securityanalyze.portfolio.domain;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository {

    TransactionRecord save(TransactionRecord transaction);

    Optional<TransactionRecord> findById(Long id);

    Optional<TransactionRecord> findByIdAndPortfolioId(Long id, Long portfolioId);

    List<TransactionRecord> findByPortfolioId(Long portfolioId, String stockCode, TradeType tradeType,
                                                 String startDate, String endDate, int offset, int limit);

    long countByPortfolioId(Long portfolioId, String stockCode, TradeType tradeType,
                            String startDate, String endDate);

    void update(TransactionRecord transaction);

    void softDelete(Long id);

    List<TransactionRecord> findActiveByPortfolioIdAndStockCode(Long portfolioId, String stockCode);
}
