package com.example.securityanalyze.index.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Table("index_info")
public class IndexInfo {

    @Id
    private Long id;

    private String indexCode;

    private String indexName;

    private String indexType;

    private String market;

    private LocalDate baseDate;

    private BigDecimal basePoint;

    private Integer componentCount;

    private LocalDate publishDate;

    private Boolean isCore;

    private String source;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
