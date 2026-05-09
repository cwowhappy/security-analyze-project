package com.example.securityanalyze.research.api;

import lombok.Data;

import java.util.List;

@Data
public class IndustryRankResponse {

    private int rank;
    private int total;
    private String sortBy;
    private String order;
    private List<IndustryRankItemDto> items;
}
