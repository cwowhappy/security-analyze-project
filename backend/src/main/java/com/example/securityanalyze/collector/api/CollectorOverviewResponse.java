package com.example.securityanalyze.collector.api;

import lombok.Data;

import java.util.List;

@Data
public class CollectorOverviewResponse {

    private List<CollectorOverviewItem> data;
}
