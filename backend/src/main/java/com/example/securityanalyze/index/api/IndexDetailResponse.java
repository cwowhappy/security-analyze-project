package com.example.securityanalyze.index.api;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class IndexDetailResponse {

    private String indexCode;

    private String indexName;

    private String indexType;

    private String market;

    private LocalDate baseDate;

    private BigDecimal basePoint;

    private Integer componentCount;

    private LocalDate publishDate;
}
