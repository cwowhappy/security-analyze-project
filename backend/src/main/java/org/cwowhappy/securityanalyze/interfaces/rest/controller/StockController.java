package org.cwowhappy.securityanalyze.interfaces.rest.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cwowhappy.securityanalyze.interfaces.rest.request.CreateStockRequest;
import org.cwowhappy.securityanalyze.interfaces.rest.response.ApiResponse;
import org.cwowhappy.securityanalyze.shared.dto.PageQuery;
import org.cwowhappy.securityanalyze.shared.dto.PageResult;
import org.cwowhappy.securityanalyze.stock.application.dto.StockDTO;
import org.cwowhappy.securityanalyze.stock.application.service.StockAppService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 股票 REST 控制器。
 */
@Slf4j
@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockAppService stockAppService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<StockDTO>>> listStocks(
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String market) {
        List<StockDTO> stocks;
        if (industry != null && !industry.isBlank()) {
            stocks = stockAppService.findByIndustry(industry);
        } else if (market != null && !market.isBlank()) {
            stocks = stockAppService.findAll(); // 当前 AppService 无 findByMarket，先返回全部
        } else {
            stocks = stockAppService.findAll();
        }
        return ResponseEntity.ok(ApiResponse.success(stocks));
    }

    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResult<StockDTO>>> pageStocks(PageQuery query) {
        PageResult<StockDTO> result = stockAppService.findByPage(query);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{stockCode}")
    public ResponseEntity<ApiResponse<StockDTO>> getStock(@PathVariable String stockCode) {
        return stockAppService.findByStockCode(stockCode)
                .map(dto -> ResponseEntity.ok(ApiResponse.success(dto)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "股票不存在: " + stockCode)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<String>> createStock(@Valid @RequestBody CreateStockRequest request) {
        StockDTO dto = StockDTO.builder()
                .stockCode(request.getStockCode())
                .name(request.getName())
                .market(request.getMarket())
                .tsCode(request.getTsCode())
                .fullName(request.getFullName())
                .exchange(request.getExchange())
                .listDate(request.getListDate())
                .industry(request.getIndustry())
                .area(request.getArea())
                .totalShares(request.getTotalShares())
                .floatShares(request.getFloatShares())
                .build();
        String id = stockAppService.createStock(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("创建成功", id));
    }
}
