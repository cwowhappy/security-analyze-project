package com.example.securityanalyze.company.api;

import lombok.Data;

import java.util.List;

@Data
public class CompanyListResponse {

    private List<CompanyListItem> items;

    private long total;

    private int page;

    private int size;
}
