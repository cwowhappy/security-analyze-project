package com.example.securityanalyze.index.api;

import lombok.Data;

import java.time.LocalDate;

@Data
public class IndexListItem {

    private String indexCode;

    private String indexName;

    private String indexType;

    private String market;

    private LocalDate publishDate;
}
