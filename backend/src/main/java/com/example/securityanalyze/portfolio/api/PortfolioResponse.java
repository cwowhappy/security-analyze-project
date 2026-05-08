package com.example.securityanalyze.portfolio.api;

import com.example.securityanalyze.portfolio.domain.PortfolioType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PortfolioResponse {
    private Long id;
    private String name;
    private PortfolioType type;
    private String broker;
    private String description;
    private LocalDateTime createdAt;
}
