package com.example.securityanalyze.company.api;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CompanyListItem {

    private String stockCode;

    private String stockName;

    private String industry;

    private String region;

    private LocalDate listingDate;

    private String market;
}
