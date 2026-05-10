package com.example.securityanalyze.company.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Table("company")
public class Company {

    @Id
    private Long id;

    private String unifiedCode;

    private String companyName;

    private String shortName;

    private String industry;

    private String region;

    private LocalDate establishDate;

    private BigDecimal registeredCapital;

    private Boolean isDeleted;

    private LocalDateTime deletedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
