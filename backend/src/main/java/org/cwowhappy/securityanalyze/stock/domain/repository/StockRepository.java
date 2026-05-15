package org.cwowhappy.securityanalyze.stock.domain.repository;

import org.cwowhappy.securityanalyze.shared.dto.PageQuery;
import org.cwowhappy.securityanalyze.shared.dto.PageResult;
import org.cwowhappy.securityanalyze.stock.domain.model.Stock;
import org.cwowhappy.securityanalyze.stock.domain.model.StockId;

import java.util.List;
import java.util.Optional;

/**
 * 股票仓库接口（Port）。
 * 定义在领域层，由基础设施层实现。
 */
public interface StockRepository {

    Optional<Stock> findById(StockId id);

    Optional<Stock> findByStockCode(String stockCode);

    List<Stock> findAll();

    PageResult<Stock> findByPage(PageQuery query, String market, String industry, String area, String keyword);

    List<Stock> findByCompanyId(String companyId);

    /**
     * 按行业查询股票列表。
     */
    List<Stock> findByIndustry(String industry);

    StockId save(Stock stock);

    void deleteById(StockId id);
}
