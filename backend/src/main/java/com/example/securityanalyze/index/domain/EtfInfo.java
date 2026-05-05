package com.example.securityanalyze.index.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Table("etf_info")
public class EtfInfo {

    @Id
    private Long id;

    private String etfCode;

    private String etfName;

    private String trackingIndexCode;

    private BigDecimal managementFee;

    private BigDecimal fundSize;

    private LocalDate establishDate;

    private String market;

    private String source;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
