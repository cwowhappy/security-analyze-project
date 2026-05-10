package org.cwowhappy.securityanalyze.stock.application.service;

import org.cwowhappy.securityanalyze.stock.application.dto.StockDTO;

import java.util.List;
import java.util.Optional;

/**
 * 股票应用服务接口。
 */
public interface StockAppService {

    Optional<StockDTO> findBySymbol(String symbol);

    List<StockDTO> findAll();

    String createStock(StockDTO dto);
}
