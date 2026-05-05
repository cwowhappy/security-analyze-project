package com.example.securityanalyze.index.api;

import com.example.securityanalyze.common.util.PageUtils;
import com.example.securityanalyze.index.application.IndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/indexes")
@RequiredArgsConstructor
public class IndexController {

    private final IndexService indexService;

    @GetMapping
    public ResponseEntity<IndexListResponse> listIndexes(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int[] normalized = PageUtils.normalize(page, size);
        page = normalized[0];
        size = normalized[1];

        log.info("查询指数列表, keyword={}, page={}, size={}", keyword, page, size);
        IndexListResponse response = indexService.listIndexes(keyword, page, size);
        log.debug("查询指数列表完成, 返回{}条记录", response.getItems().size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{indexCode}")
    public ResponseEntity<IndexDetailResponse> getIndexDetail(
            @PathVariable String indexCode) {

        log.info("查询指数详情, indexCode={}", indexCode);
        Optional<IndexDetailResponse> detail = indexService.getIndexDetail(indexCode);
        return detail.map(ResponseEntity::ok)
                .orElseGet(() -> {
                    log.warn("指数不存在, indexCode={}", indexCode);
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping("/{indexCode}/trend")
    public ResponseEntity<IndexTrendResponse> getIndexTrend(
            @PathVariable String indexCode,
            @RequestParam(defaultValue = "day") String granularity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("查询指数趋势, indexCode={}, granularity={}, startDate={}, endDate={}",
                indexCode, granularity, startDate, endDate);
        IndexTrendResponse response = indexService.getIndexTrend(indexCode, granularity, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{indexCode}/etfs")
    public ResponseEntity<List<EtfListItem>> getIndexEtfs(
            @PathVariable String indexCode) {

        log.info("查询指数关联ETF, indexCode={}", indexCode);
        List<EtfListItem> items = indexService.getIndexEtfs(indexCode);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<IndexCategoryGroup>> getIndexCategories() {
        log.info("查询分类核心指数");
        List<IndexCategoryGroup> groups = indexService.getIndexCategories();
        log.debug("查询分类核心指数完成, 返回{}个分组", groups.size());
        return ResponseEntity.ok(groups);
    }
}
