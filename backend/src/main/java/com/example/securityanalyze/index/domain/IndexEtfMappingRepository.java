package com.example.securityanalyze.index.domain;

import java.util.List;

public interface IndexEtfMappingRepository {

    List<IndexEtfMapping> findByIndexCode(String indexCode);

    List<IndexEtfMapping> findByEtfCode(String etfCode);
}
