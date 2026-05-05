package com.example.securityanalyze.index.api;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class EtfListItem {

    private String etfCode;

    private String etfName;

    private String trackingIndexCode;

    private BigDecimal managementFee;

    private BigDecimal fundSize;

    private LocalDate establishDate;

    private String market;
}
