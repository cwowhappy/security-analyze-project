package com.example.securityanalyze.portfolio.domain;

public class PortfolioNotFoundException extends RuntimeException {
    public PortfolioNotFoundException(String message) {
        super(message);
    }
}
