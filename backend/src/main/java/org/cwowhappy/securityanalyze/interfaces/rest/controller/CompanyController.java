package org.cwowhappy.securityanalyze.interfaces.rest.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cwowhappy.securityanalyze.company.application.dto.CompanyDTO;
import org.cwowhappy.securityanalyze.company.application.service.CompanyAppService;
import org.cwowhappy.securityanalyze.interfaces.rest.request.CreateCompanyRequest;
import org.cwowhappy.securityanalyze.interfaces.rest.response.ApiResponse;
import org.cwowhappy.securityanalyze.shared.dto.PageQuery;
import org.cwowhappy.securityanalyze.shared.dto.PageResult;
import org.cwowhappy.securityanalyze.shared.dto.StockBriefDTO;
import org.cwowhappy.securityanalyze.stock.application.dto.StockDTO;
import org.cwowhappy.securityanalyze.stock.application.service.StockAppService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 公司 REST 控制器。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyAppService companyAppService;
    private final StockAppService stockAppService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<CompanyDTO>>> listCompanies(
            PageQuery pageQuery,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String controllerType,
            @RequestParam(required = false) String keyword) {
        PageResult<CompanyDTO> result = companyAppService.findByPage(pageQuery, industry, province, controllerType, keyword);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CompanyDTO>> getCompany(@PathVariable String id) {
        return companyAppService.findById(id)
                .map(dto -> {
                    List<StockDTO> stocks = stockAppService.findByCompanyId(dto.getId());
                    List<StockBriefDTO> briefStocks = stocks.stream()
                            .map(s -> StockBriefDTO.builder()
                                    .stockCode(s.getStockCode())
                                    .name(s.getName())
                                    .market(s.getMarket())
                                    .exchange(s.getExchange())
                                    .listDate(s.getListDate())
                                    .build())
                            .toList();
                    dto.setStocks(briefStocks);
                    return ResponseEntity.ok(ApiResponse.success(dto));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "公司不存在: " + id)));
    }

    /**
     * 通过股票代码查询公司信息（备用接口，用于统一社会信用代码缺失场景）。
     */
    @GetMapping("/by-stock/{stockCode}")
    public ResponseEntity<ApiResponse<CompanyDTO>> getCompanyByStockCode(@PathVariable String stockCode) {
        Optional<StockDTO> stockOpt = stockAppService.findByStockCode(stockCode);
        if (stockOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "股票不存在: " + stockCode));
        }
        StockDTO stock = stockOpt.get();
        if (stock.getCompanyId() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "股票未关联公司信息: " + stockCode));
        }
        return companyAppService.findById(stock.getCompanyId())
                .map(dto -> {
                    List<StockDTO> stocks = stockAppService.findByCompanyId(dto.getId());
                    List<StockBriefDTO> briefStocks = stocks.stream()
                            .map(s -> StockBriefDTO.builder()
                                    .stockCode(s.getStockCode())
                                    .name(s.getName())
                                    .market(s.getMarket())
                                    .exchange(s.getExchange())
                                    .listDate(s.getListDate())
                                    .build())
                            .toList();
                    dto.setStocks(briefStocks);
                    return ResponseEntity.ok(ApiResponse.success(dto));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "公司不存在: " + stock.getCompanyId())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<String>> createCompany(@Valid @RequestBody CreateCompanyRequest request) {
        CompanyDTO dto = CompanyDTO.builder()
                .unifiedSocialCreditCode(request.getUnifiedSocialCreditCode())
                .name(request.getName())
                .shortName(request.getShortName())
                .englishName(request.getEnglishName())
                .formerName(request.getFormerName())
                .legalRepresentative(request.getLegalRepresentative())
                .chairman(request.getChairman())
                .manager(request.getManager())
                .secretary(request.getSecretary())
                .regCapital(request.getRegCapital())
                .setupDate(request.getSetupDate())
                .province(request.getProvince())
                .city(request.getCity())
                .regAddress(request.getRegAddress())
                .officeAddress(request.getOfficeAddress())
                .website(request.getWebsite())
                .industry(request.getIndustry())
                .mainBusiness(request.getMainBusiness())
                .businessScope(request.getBusinessScope())
                .introduction(request.getIntroduction())
                .employees(request.getEmployees())
                .controllerName(request.getControllerName())
                .controllerType(request.getControllerType())
                .build();
        String id = companyAppService.createCompany(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("创建成功", id));
    }
}
