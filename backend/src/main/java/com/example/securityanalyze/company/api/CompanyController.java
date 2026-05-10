package com.example.securityanalyze.company.api;

import com.example.securityanalyze.common.util.PageUtils;
import com.example.securityanalyze.company.application.CompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping
    public ResponseEntity<CompanyListResponse> listCompanies(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int[] normalized = PageUtils.normalize(page, size);
        page = normalized[0];
        size = normalized[1];

        log.info("查询公司列表, keyword={}, page={}, size={}", keyword, page, size);
        CompanyListResponse response = companyService.listCompanies(keyword, page, size);
        log.debug("查询公司列表完成, 返回{}条记录", response.getItems().size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{stockCode}")
    public ResponseEntity<CompanyDetailResponse> getCompanyDetail(
            @PathVariable String stockCode) {

        log.info("查询公司详情, stockCode={}", stockCode);
        Optional<CompanyDetailResponse> detail = companyService.getCompanyDetail(stockCode);
        return detail.map(d -> {
                    log.debug("查询公司详情成功, stockCode={}", stockCode);
                    return ResponseEntity.ok(d);
                })
                .orElseGet(() -> {
                    log.warn("公司不存在, stockCode={}", stockCode);
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping("/batch")
    public ResponseEntity<List<CompanyListItem>> batchQuery(
            @RequestBody List<String> stockCodes) {
        log.info("批量查询公司, stockCodes={}", stockCodes);
        return ResponseEntity.ok(companyService.batchQuery(stockCodes));
    }
}
