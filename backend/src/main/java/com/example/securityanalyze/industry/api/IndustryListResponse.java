package com.example.securityanalyze.industry.api;

import lombok.Data;

import java.util.List;

@Data
public class IndustryListResponse {

    private List<IndustryListItem> data;
    private int total;
}
