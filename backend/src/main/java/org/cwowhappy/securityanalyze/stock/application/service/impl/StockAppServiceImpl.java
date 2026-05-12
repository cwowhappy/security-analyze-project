package org.cwowhappy.securityanalyze.stock.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cwowhappy.securityanalyze.shared.dto.PageQuery;
import org.cwowhappy.securityanalyze.shared.dto.PageResult;
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
    public Optional<StockDTO> findByStockCode(String stockCode) {
        log.debug("查询股票: stockCode={}", stockCode);
        return stockRepository.findByStockCode(stockCode)
                .map(this::toDTO);
    }

    @Override
    public PageResult<StockDTO> findByPage(PageQuery query, String market, String industry, String area, String keyword) {
        log.debug("分页查询股票: page={}, size={}, market={}, industry={}, area={}, keyword={}",
                query.getPage(), query.getSize(), market, industry, area, keyword);
        PageResult<Stock> pageResult = stockRepository.findByPage(query, market, industry, area, keyword);
        List<StockDTO> dtos = pageResult.getList().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return PageResult.<StockDTO>builder()
                .list(dtos)
                .total(pageResult.getTotal())
                .page(pageResult.getPage())
                .size(pageResult.getSize())
                .build();
    }

    @Override
    public List<StockDTO> findByCompanyId(String companyId) {
        log.debug("按公司ID查询股票: companyId={}", companyId);
        return stockRepository.findByCompanyId(companyId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public String createStock(StockDTO dto) {
        log.info("创建股票: stockCode={}", dto.getStockCode());
        Stock stock = Stock.builder()
                .id(StockId.generate())
                .stockCode(dto.getStockCode())
                .name(dto.getName())
                .market(dto.getMarket())
                .tsCode(dto.getTsCode())
                .fullName(dto.getFullName())
                .exchange(dto.getExchange())
                .listDate(dto.getListDate())
                .industry(dto.getIndustry())
                .area(dto.getArea())
                .totalShares(dto.getTotalShares())
                .floatShares(dto.getFloatShares())
                .companyId(dto.getCompanyId())
                .build();
        StockId id = stockRepository.save(stock);
        log.info("股票创建成功: id={}", id);
        return id.getValue();
    }

    private StockDTO toDTO(Stock stock) {
        return StockDTO.builder()
                .id(stock.getId().getValue())
                .stockCode(stock.getStockCode())
                .name(stock.getName())
                .market(stock.getMarket())
                .tsCode(stock.getTsCode())
                .fullName(stock.getFullName())
                .exchange(stock.getExchange())
                .listDate(stock.getListDate())
                .industry(stock.getIndustry())
                .area(stock.getArea())
                .totalShares(stock.getTotalShares())
                .floatShares(stock.getFloatShares())
                .companyId(stock.getCompanyId())
                .createdAt(stock.getCreatedAt())
                .updatedAt(stock.getUpdatedAt())
                .build();
    }
}
