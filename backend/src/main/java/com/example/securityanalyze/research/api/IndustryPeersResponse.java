package com.example.securityanalyze.research.api;

import lombok.Data;

import java.util.List;

@Data
public class IndustryPeersResponse {

    private List<PeerMetricDto> peers;
}
