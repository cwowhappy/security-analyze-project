package com.example.securityanalyze.index.api;

import lombok.Data;

import java.util.List;

@Data
public class IndexCategoryGroup {

    private String indexType;

    private String indexTypeLabel;

    private List<IndexListItem> items;
}
