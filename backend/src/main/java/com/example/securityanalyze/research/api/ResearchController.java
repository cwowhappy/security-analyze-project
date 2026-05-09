package com.example.securityanalyze.research.api;

import com.example.securityanalyze.common.util.PageUtils;
import com.example.securityanalyze.research.application.FundamentalAnalysisService;
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
@RequestMapping("/api/research")
@RequiredArgsConstructor
public class ResearchController {

    private final FundamentalAnalysisService fundamentalAnalysisService;

    @GetMapping("/fundamental/overview/{stockCode}")
    public ResponseEntity<FundamentalOverviewResponse> getOverview(
            @PathVariable String stockCode) {
        log.info("查询基本面概览, stockCode={}", stockCode);
        return fundamentalAnalysisService.getOverview(stockCode)
                .map(d -> {
                    log.debug("查询基本面概览成功, stockCode={}", stockCode);
                    return ResponseEntity.ok(d);
                })
                .orElseGet(() -> {
                    log.warn("公司基本面数据不存在, stockCode={}", stockCode);
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping("/fundamental/screen")
    public ResponseEntity<FundamentalScreenResponse> screenCompanies(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String market,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("筛选公司, keyword={}, industry={}, market={}, page={}, size={}",
                keyword, industry, market, page, size);
        int[] normalized = PageUtils.normalize(page, size);
        FundamentalScreenResponse response = fundamentalAnalysisService.screenCompanies(
                keyword, industry, market, normalized[0], normalized[1]);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/fundamental/industry-peers/{stockCode}")
    public ResponseEntity<IndustryPeersResponse> getIndustryPeers(
            @PathVariable String stockCode) {
        log.info("查询同行业对比, stockCode={}", stockCode);
        IndustryPeersResponse response = fundamentalAnalysisService.getIndustryPeers(stockCode);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/fundamental/industry-rank/{stockCode}")
    public ResponseEntity<IndustryRankResponse> getIndustryRank(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "roe") String sortBy,
            @RequestParam(defaultValue = "desc") String order) {
        log.info("查询行业排名, stockCode={}, sortBy={}, order={}", stockCode, sortBy, order);
        IndustryRankResponse response = fundamentalAnalysisService.getIndustryRank(stockCode, sortBy, order);
        return ResponseEntity.ok(response);
    }
}
