package org.cwowhappy.securityanalyze.interfaces.rest.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cwowhappy.securityanalyze.company.application.dto.CompanyDTO;
import org.cwowhappy.securityanalyze.shared.dto.CompanyBriefDTO;
import org.cwowhappy.securityanalyze.company.application.service.CompanyAppService;
import org.cwowhappy.securityanalyze.interfaces.rest.request.CreateStockRequest;
import org.cwowhappy.securityanalyze.interfaces.rest.response.ApiResponse;
import org.cwowhappy.securityanalyze.shared.dto.PageQuery;
import org.cwowhappy.securityanalyze.shared.dto.PageResult;
import org.cwowhappy.securityanalyze.stock.application.dto.StockDTO;
import org.cwowhappy.securityanalyze.stock.application.service.StockAppService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 股票 REST 控制器。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockAppService stockAppService;
    private final CompanyAppService companyAppService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<StockDTO>>> listStocks(
            PageQuery query,
            @RequestParam(required = false) String market,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String keyword) {
        PageResult<StockDTO> result = stockAppService.findByPage(query, market, industry, area, keyword);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{stockCode}")
    public ResponseEntity<ApiResponse<StockDTO>> getStock(@PathVariable String stockCode) {
        return stockAppService.findByStockCode(stockCode)
                .map(dto -> {
                    if (dto.getCompanyId() != null) {
                        companyAppService.findById(dto.getCompanyId())
                                .ifPresent(company -> dto.setCompany(toBrief(company)));
                    }
                    return ResponseEntity.ok(ApiResponse.success(dto));
                })
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
                .companyId(request.getCompanyId())
                .build();
        String id = stockAppService.createStock(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("创建成功", id));
    }

    private CompanyBriefDTO toBrief(CompanyDTO company) {
        return CompanyBriefDTO.builder()
                .id(company.getId())
                .unifiedSocialCreditCode(company.getUnifiedSocialCreditCode())
                .name(company.getName())
                .shortName(company.getShortName())
                .legalRepresentative(company.getLegalRepresentative())
                .regCapital(company.getRegCapital())
                .setupDate(company.getSetupDate())
                .mainBusiness(company.getMainBusiness())
                .build();
    }
}
