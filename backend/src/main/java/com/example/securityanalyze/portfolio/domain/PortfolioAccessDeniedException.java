package com.example.securityanalyze.portfolio.domain;

public class PortfolioAccessDeniedException extends RuntimeException {
    public PortfolioAccessDeniedException(String message) {
        super(message);
    }
}
