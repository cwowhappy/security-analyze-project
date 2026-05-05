package com.example.securityanalyze.index.application;

import com.example.securityanalyze.index.api.*;
import com.example.securityanalyze.index.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexService {

    private final IndexRepository indexRepository;
    private final IndexHistoryRepository indexHistoryRepository;
    private final EtfInfoRepository etfInfoRepository;
    private final IndexEtfMappingRepository indexEtfMappingRepository;

    public IndexListResponse listIndexes(String keyword, int page, int size) {
        log.debug("查询指数列表, keyword={}, page={}, size={}", keyword, page, size);
        int offset = page * size;

        List<IndexInfo> indexes = indexRepository.findByKeyword(keyword, offset, size);
        long total = indexRepository.countByKeyword(keyword);

        List<IndexListItem> items = indexes.stream()
                .map(this::toListItem)
                .toList();

        IndexListResponse response = new IndexListResponse();
        response.setItems(items);
        response.setTotal(total);
        response.setPage(page);
        response.setSize(size);
        log.info("查询指数列表完成, keyword={}, 返回{}条记录", keyword, items.size());
        return response;
    }

    public Optional<IndexDetailResponse> getIndexDetail(String indexCode) {
        log.debug("查询指数详情, indexCode={}", indexCode);
        Optional<IndexInfo> indexOpt = indexRepository.findByIndexCode(indexCode);
        if (indexOpt.isEmpty()) {
            log.warn("指数不存在, indexCode={}", indexCode);
            return Optional.empty();
        }
        log.info("查询指数详情成功, indexCode={}", indexCode);
        return Optional.of(toDetailResponse(indexOpt.get()));
    }

    public IndexTrendResponse getIndexTrend(String indexCode, String granularity,
                                             LocalDate startDate, LocalDate endDate) {
        log.debug("查询指数趋势, indexCode={}, granularity={}, startDate={}, endDate={}",
                indexCode, granularity, startDate, endDate);

        List<IndexHistory> histories = indexHistoryRepository.findByIndexCodeAndGranularity(
                indexCode, granularity, startDate, endDate);

        List<IndexTrendItem> items = histories.stream()
                .map(this::toTrendItem)
                .toList();

        IndexTrendResponse response = new IndexTrendResponse();
        response.setIndexCode(indexCode);
        response.setGranularity(granularity);
        response.setItems(items);
        log.info("查询指数趋势完成, indexCode={}, granularity={}, 返回{}条记录",
                indexCode, granularity, items.size());
        return response;
    }

    public List<EtfListItem> getIndexEtfs(String indexCode) {
        log.debug("查询指数关联ETF, indexCode={}", indexCode);

        // 优先通过映射表查询
        List<IndexEtfMapping> mappings = indexEtfMappingRepository.findByIndexCode(indexCode);
        List<EtfListItem> items;

        if (!mappings.isEmpty()) {
            List<String> etfCodes = mappings.stream()
                    .map(IndexEtfMapping::getEtfCode)
                    .distinct()
                    .toList();
            List<EtfInfo> etfs = etfInfoRepository.findByEtfCodes(etfCodes);
            items = etfs.stream().map(this::toEtfItem).toList();
        } else {
            // 降级：通过 tracking_index_code 模糊匹配
            List<EtfInfo> etfs = etfInfoRepository.findByTrackingIndexCode(indexCode);
            items = etfs.stream().map(this::toEtfItem).toList();
        }

        log.info("查询指数关联ETF完成, indexCode={}, 返回{}条记录", indexCode, items.size());
        return items;
    }

    public List<IndexCategoryGroup> getIndexCategories() {
        log.debug("查询分类核心指数");

        // 定义类型顺序和标签
        String[][] typeOrder = {
            {"宽基", "宽基指数"},
            {"行业", "行业指数"},
            {"主题", "主题指数"},
            {"策略", "策略指数"},
            {"其他", "其他指数"},
        };

        List<IndexCategoryGroup> groups = new ArrayList<>();
        for (String[] typePair : typeOrder) {
            String typeCode = typePair[0];
            String typeLabel = typePair[1];

            List<IndexInfo> indexes = indexRepository.findCoreByType(typeCode);
            if (indexes.isEmpty()) {
                continue;
            }

            List<IndexListItem> items = indexes.stream()
                    .map(this::toListItem)
                    .toList();

            IndexCategoryGroup group = new IndexCategoryGroup();
            group.setIndexType(typeCode);
            group.setIndexTypeLabel(typeLabel);
            group.setItems(items);
            groups.add(group);
        }

        log.info("查询分类核心指数完成, 返回{}个分组", groups.size());
        return groups;
    }

    private IndexListItem toListItem(IndexInfo index) {
        IndexListItem item = new IndexListItem();
        item.setIndexCode(index.getIndexCode());
        item.setIndexName(index.getIndexName());
        item.setIndexType(index.getIndexType());
        item.setMarket(index.getMarket());
        item.setPublishDate(index.getPublishDate());
        return item;
    }

    private IndexDetailResponse toDetailResponse(IndexInfo index) {
        IndexDetailResponse response = new IndexDetailResponse();
        response.setIndexCode(index.getIndexCode());
        response.setIndexName(index.getIndexName());
        response.setIndexType(index.getIndexType());
        response.setMarket(index.getMarket());
        response.setBaseDate(index.getBaseDate());
        response.setBasePoint(index.getBasePoint());
        response.setComponentCount(index.getComponentCount());
        response.setPublishDate(index.getPublishDate());
        return response;
    }

    private IndexTrendItem toTrendItem(IndexHistory history) {
        IndexTrendItem item = new IndexTrendItem();
        item.setTradeDate(history.getTradeDate());
        item.setOpenPrice(history.getOpenPrice());
        item.setHighPrice(history.getHighPrice());
        item.setLowPrice(history.getLowPrice());
        item.setClosePrice(history.getClosePrice());
        item.setVolume(history.getVolume());
        item.setAmount(history.getAmount());
        item.setAmplitude(history.getAmplitude());
        item.setChangePct(history.getChangePct());
        item.setChangeAmount(history.getChangeAmount());
        item.setTurnoverRate(history.getTurnoverRate());
        return item;
    }

    private EtfListItem toEtfItem(EtfInfo etf) {
        EtfListItem item = new EtfListItem();
        item.setEtfCode(etf.getEtfCode());
        item.setEtfName(etf.getEtfName());
        item.setTrackingIndexCode(etf.getTrackingIndexCode());
        item.setManagementFee(etf.getManagementFee());
        item.setFundSize(etf.getFundSize());
        item.setEstablishDate(etf.getEstablishDate());
        item.setMarket(etf.getMarket());
        return item;
    }
}
