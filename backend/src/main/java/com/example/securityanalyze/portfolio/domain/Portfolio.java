package com.example.securityanalyze.portfolio.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Table("portfolio")
public class Portfolio {
    @Id
    private Long id;
    private Long userId;
    private String name;
    private PortfolioType type;
    private String broker;
    private String description;
    private Boolean isDeleted;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
