package com.example.securityanalyze.collector.api;

import lombok.Data;

import java.util.List;

@Data
public class CollectorTaskListResponse {

    private List<CollectorTaskItem> data;
    private long total;
    private int page;
    private int size;
}
