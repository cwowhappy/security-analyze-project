package com.example.securityanalyze.research.api;

import lombok.Data;

import java.util.List;

@Data
public class FundamentalScreenResponse {

    private List<ScreenCompanyItemResponse> items;
    private long total;
    private int page;
    private int size;
}
