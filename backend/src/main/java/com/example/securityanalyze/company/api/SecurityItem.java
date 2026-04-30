package com.example.securityanalyze.company.api;

import lombok.Data;

import java.time.LocalDate;

@Data
public class SecurityItem {

    private String stockCode;

    private String stockName;

    private String market;

    private String securityType;

    private LocalDate listingDate;

    private String listingStatus;
}
