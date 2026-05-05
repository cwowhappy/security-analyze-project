package com.example.securityanalyze.index.api;

import lombok.Data;

import java.util.List;

@Data
public class IndexTrendResponse {

    private String indexCode;

    private String granularity;

    private List<IndexTrendItem> items;
}
