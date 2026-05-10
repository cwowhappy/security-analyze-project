package org.cwowhappy.securityanalyze.interfaces.rest.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cwowhappy.securityanalyze.interfaces.rest.request.CreateStockRequest;
import org.cwowhappy.securityanalyze.interfaces.rest.response.ApiResponse;
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
    public ResponseEntity<ApiResponse<List<StockDTO>>> listStocks() {
        List<StockDTO> stocks = stockAppService.findAll();
        return ResponseEntity.ok(ApiResponse.success(stocks));
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<ApiResponse<StockDTO>> getStock(@PathVariable String symbol) {
        return stockAppService.findBySymbol(symbol)
                .map(dto -> ResponseEntity.ok(ApiResponse.success(dto)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "股票不存在: " + symbol)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<String>> createStock(@Valid @RequestBody CreateStockRequest request) {
        StockDTO dto = StockDTO.builder()
                .symbol(request.getSymbol())
                .name(request.getName())
                .market(request.getMarket())
                .currentPrice(request.getCurrentPrice())
                .changePercent(request.getChangePercent())
                .build();
        String id = stockAppService.createStock(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("创建成功", id));
    }
}
