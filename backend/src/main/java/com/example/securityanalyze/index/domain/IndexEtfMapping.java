package com.example.securityanalyze.index.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Table("index_etf_mapping")
public class IndexEtfMapping {

    @Id
    private Long id;

    private String indexCode;

    private String etfCode;

    private String relationType;

    private LocalDateTime createdAt;
}
