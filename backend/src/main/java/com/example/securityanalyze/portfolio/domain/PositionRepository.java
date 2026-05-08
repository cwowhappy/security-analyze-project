package com.example.securityanalyze.portfolio.domain;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PositionRepository {

    Position save(Position position);

    Optional<Position> findById(Long id);

    Optional<Position> findByPortfolioIdAndStockCode(Long portfolioId, String stockCode);

    List<Position> findByPortfolioId(Long portfolioId);

    List<Map<String, Object>> findByPortfolioIdWithQuote(Long portfolioId);

    void update(Position position);

    void softDelete(Long id);

    void softDeleteByPortfolioIdAndStockCode(Long portfolioId, String stockCode);
}
