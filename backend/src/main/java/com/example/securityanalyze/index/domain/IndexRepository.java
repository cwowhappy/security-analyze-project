package com.example.securityanalyze.index.domain;

import java.util.List;
import java.util.Optional;

public interface IndexRepository {

    List<IndexInfo> findByKeyword(String keyword, int offset, int limit);

    long countByKeyword(String keyword);

    Optional<IndexInfo> findByIndexCode(String indexCode);

    Optional<IndexInfo> findById(Long id);

    List<IndexInfo> findAllByIndexCodes(List<String> indexCodes);

    List<IndexInfo> findCoreByType(String indexType);
}
