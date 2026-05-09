package com.example.securityanalyze.company.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Table("company_security")
public class CompanySecurity {

    @Id
    private Long id;

    private Long companyId;

    private String stockCode;

    private String stockName;

    private String market;

    private String securityType;

    private LocalDate listingDate;

    private String listingStatus;

    private Boolean isDeleted;

    private LocalDateTime deletedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
