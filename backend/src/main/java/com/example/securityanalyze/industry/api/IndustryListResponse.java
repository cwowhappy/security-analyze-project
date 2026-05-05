package com.example.securityanalyze.industry.api;

import lombok.Data;

import java.util.List;

@Data
public class IndustryListResponse {

    private String standard;
    private Integer level;
    private List<IndustryCategoryDto> data;
    private int total;
}
