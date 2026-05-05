package com.example.securityanalyze.index.api;

import lombok.Data;

import java.util.List;

@Data
public class IndexListResponse {

    private List<IndexListItem> items;

    private long total;

    private int page;

    private int size;
}
