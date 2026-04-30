package com.example.securityanalyze.company.api;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CompanyDetailResponse {

    private String stockCode;

    private String stockName;

    private String industry;

    private String region;

    private LocalDate establishDate;

    private BigDecimal registeredCapital;

    private LocalDate listingDate;

    private String market;

    private List<SecurityItem> securities;
}
