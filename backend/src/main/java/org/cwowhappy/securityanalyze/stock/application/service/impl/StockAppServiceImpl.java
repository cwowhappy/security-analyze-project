package org.cwowhappy.securityanalyze.stock.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cwowhappy.securityanalyze.stock.application.dto.StockDTO;
import org.cwowhappy.securityanalyze.stock.application.service.StockAppService;
import org.cwowhappy.securityanalyze.stock.domain.model.Stock;
import org.cwowhappy.securityanalyze.stock.domain.model.StockId;
import org.cwowhappy.securityanalyze.stock.domain.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 股票应用服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockAppServiceImpl implements StockAppService {

    private final StockRepository stockRepository;

    @Override
    public Optional<StockDTO> findBySymbol(String symbol) {
        log.debug("查询股票: symbol={}", symbol);
        return stockRepository.findBySymbol(symbol)
                .map(this::toDTO);
    }

    @Override
    public List<StockDTO> findAll() {
        log.debug("查询所有股票");
        return stockRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public String createStock(StockDTO dto) {
        log.info("创建股票: symbol={}", dto.getSymbol());
        Stock stock = Stock.builder()
                .id(StockId.generate())
                .symbol(dto.getSymbol())
                .name(dto.getName())
                .market(dto.getMarket())
                .currentPrice(dto.getCurrentPrice())
                .changePercent(dto.getChangePercent())
                .build();
        StockId id = stockRepository.save(stock);
        log.info("股票创建成功: id={}", id);
        return id.getValue();
    }

    private StockDTO toDTO(Stock stock) {
        return StockDTO.builder()
                .id(stock.getId().getValue())
                .symbol(stock.getSymbol())
                .name(stock.getName())
                .market(stock.getMarket())
                .currentPrice(stock.getCurrentPrice())
                .changePercent(stock.getChangePercent())
                .updatedAt(stock.getUpdatedAt())
                .build();
    }
}
