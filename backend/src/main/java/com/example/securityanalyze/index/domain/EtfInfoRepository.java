package com.example.securityanalyze.index.domain;

import java.util.List;
import java.util.Optional;

public interface EtfInfoRepository {

    List<EtfInfo> findByTrackingIndexCode(String indexCode);

    Optional<EtfInfo> findByEtfCode(String etfCode);

    List<EtfInfo> findByEtfCodes(List<String> etfCodes);
}
