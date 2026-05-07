package com.example.securityanalyze.portfolio.domain;

import java.util.List;
import java.util.Optional;

public interface PortfolioRepository {

    Portfolio save(Portfolio portfolio);

    Optional<Portfolio> findById(Long id);

    Optional<Portfolio> findByIdAndUserId(Long id, Long userId);

    List<Portfolio> findByUserId(Long userId);

    void update(Portfolio portfolio);

    void softDelete(Long id);

    void softDeleteByUserId(Long portfolioId, Long userId);
}
