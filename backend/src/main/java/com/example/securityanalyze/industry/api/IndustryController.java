package com.example.securityanalyze.industry.api;

import com.example.securityanalyze.common.util.PageUtils;
import com.example.securityanalyze.company.api.CompanyListItem;
import com.example.securityanalyze.company.api.CompanyListResponse;
import com.example.securityanalyze.industry.application.IndustryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/industries")
@RequiredArgsConstructor
public class IndustryController {

    private final IndustryService industryService;

    @GetMapping
    public ResponseEntity<IndustryListResponse> listIndustries() {
        log.debug("查询行业列表");
        IndustryListResponse response = industryService.listIndustries();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{industryName}/companies")
    public ResponseEntity<CompanyListResponse> listCompaniesByIndustry(
            @PathVariable String industryName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int[] normalized = PageUtils.normalize(page, size);
        page = normalized[0];
        size = normalized[1];

        log.info("按行业查询公司, industryName={}, page={}, size={}", industryName, page, size);
        CompanyListResponse response = industryService.listCompaniesByIndustry(industryName, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{industryName}/trend")
    public ResponseEntity<IndustryTrendResponse> getIndustryTrend(
            @PathVariable String industryName,
            @RequestParam(defaultValue = "3m") String period) {

        log.info("查询行业趋势, industryName={}, period={}", industryName, period);
        IndustryTrendResponse response = industryService.getIndustryTrend(industryName, period);
        return ResponseEntity.ok(response);
    }
}
