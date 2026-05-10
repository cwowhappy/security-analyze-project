package org.cwowhappy.securityanalyze.stock.application.service;

import org.cwowhappy.securityanalyze.shared.dto.PageQuery;
import org.cwowhappy.securityanalyze.shared.dto.PageResult;
import org.cwowhappy.securityanalyze.stock.application.dto.StockDTO;

import java.util.List;
import java.util.Optional;

/**
 * 股票应用服务接口。
 */
public interface StockAppService {

    Optional<StockDTO> findByStockCode(String stockCode);

    List<StockDTO> findAll();

    PageResult<StockDTO> findByPage(PageQuery query);

    List<StockDTO> findByIndustry(String industry);

    String createStock(StockDTO dto);
}
