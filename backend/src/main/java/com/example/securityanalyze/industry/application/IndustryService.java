package com.example.securityanalyze.industry.application;

import com.example.securityanalyze.company.api.CompanyListItem;
import com.example.securityanalyze.company.api.CompanyListResponse;
import com.example.securityanalyze.company.domain.Company;
import com.example.securityanalyze.company.domain.CompanyRepository;
import com.example.securityanalyze.company.domain.CompanySecurity;
import com.example.securityanalyze.company.domain.CompanySecurityRepository;
import com.example.securityanalyze.industry.api.IndustryCategoryDto;
import com.example.securityanalyze.industry.api.IndustryListResponse;
import com.example.securityanalyze.industry.api.IndustryTrendResponse;
import com.example.securityanalyze.industry.api.TrendDataPoint;
import com.example.securityanalyze.industry.domain.*;
import com.example.securityanalyze.industry.infrastructure.IndustryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndustryService {

    private final IndustryRepository industryRepository;
    private final IndustryCategoryRepository industryCategoryRepository;
    private final CompanyIndustryMappingRepository companyIndustryMappingRepository;
    private final CompanyRepository companyRepository;
    private final CompanySecurityRepository companySecurityRepository;
    private final IndustryTrendGateway industryTrendGateway;

    public IndustryListResponse listIndustries(String standardCode, Integer level, String parentCode) {
        log.debug("查询行业列表, standard={}, level={}, parentCode={}", standardCode, level, parentCode);

        List<IndustryCategoryDto> dtos = new ArrayList<>();

        if (level != null && level > 0) {
            // 按指定层级查询
            List<IndustryCategory> categories = industryCategoryRepository.findByStandardAndLevelWithCount(standardCode, level);
            // 如果有 parentCode，则过滤
            if (parentCode != null && !parentCode.isBlank()) {
                categories = categories.stream()
                        .filter(c -> parentCode.equals(c.getParentCode()))
                        .toList();
            }
            for (IndustryCategory c : categories) {
                dtos.add(toDto(c));
            }
        } else {
            // 默认查询该标准下全部（先一级后二级）
            List<IndustryCategory> categories = industryCategoryRepository.findByStandard(standardCode);
            for (IndustryCategory c : categories) {
                dtos.add(toDto(c));
            }
        }

        IndustryListResponse response = new IndustryListResponse();
        response.setStandard(standardCode);
        response.setLevel(level);
        response.setData(dtos);
        response.setTotal(dtos.size());
        return response;
    }

    public CompanyListResponse listCompaniesByIndustry(String standardCode, String level1Code, String level2Code, int page, int size) {
        int offset = page * size;

        List<Long> companyIds;
        long total;
        if (level2Code != null && !level2Code.isBlank()) {
            companyIds = companyIndustryMappingRepository.findCompanyIdsByStandardAndLevel2(standardCode, level2Code);
            total = companyIndustryMappingRepository.countByStandardAndLevel2(standardCode, level2Code);
        } else {
            companyIds = companyIndustryMappingRepository.findCompanyIdsByStandardAndLevel1(standardCode, level1Code);
            total = companyIndustryMappingRepository.countByStandardAndLevel1(standardCode, level1Code);
        }

        // 分页
        int fromIndex = Math.min(offset, companyIds.size());
        int toIndex = Math.min(offset + size, companyIds.size());
        List<Long> pageIds = companyIds.subList(fromIndex, toIndex);

        List<CompanyListItem> items = new ArrayList<>();
        if (!pageIds.isEmpty()) {
            List<Company> companies = companyRepository.findAllById(pageIds);
            List<CompanySecurity> securities = companySecurityRepository.findByCompanyIds(pageIds);
            var securityMap = securities.stream()
                    .collect(java.util.stream.Collectors.toMap(CompanySecurity::getCompanyId, s -> s, (a, b) -> a));
            for (Company company : companies) {
                items.add(toListItem(company, securityMap.get(company.getId())));
            }
        }

        CompanyListResponse response = new CompanyListResponse();
        response.setItems(items);
        response.setTotal(total);
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    public IndustryTrendResponse getIndustryTrend(String standardCode, String industryCode, String period) {
        IndustryTrendResponse response = new IndustryTrendResponse();
        response.setStandard(standardCode);
        response.setIndustryCode(industryCode);
        response.setPeriod(period);

        // 获取行业名称
        IndustryCategory category = industryCategoryRepository.findByCode(standardCode, industryCode).orElse(null);
        String industryName = category != null ? category.getName() : industryCode;
        response.setIndustryName(industryName);

        // EM 标准：使用现有东财趋势网关
        if ("EM".equals(standardCode)) {
            List<TrendDataPoint> realData = industryTrendGateway.fetchTrend(industryName, period);
            if (!realData.isEmpty()) {
                response.setData(realData);
                response.setFallback(false);
                return response;
            }
        }

        // SW 标准或其他：fallback 到模拟数据（申万趋势作为增强项，后续可扩展 SwIndustryTrendGateway）
        log.warn("无法获取真实趋势数据, 返回模拟数据, standard={}, industry={}, period={}", standardCode, industryName, period);
        response.setData(generateFallbackTrend(period));
        response.setFallback(true);
        return response;
    }

    private IndustryCategoryDto toDto(IndustryCategory category) {
        IndustryCategoryDto dto = new IndustryCategoryDto();
        dto.setCode(category.getCode());
        dto.setName(category.getName());
        dto.setLevel(category.getLevel());
        dto.setParentCode(category.getParentCode());
        dto.setCompanyCount(category.getCompanyCount() != null ? category.getCompanyCount() : 0);
        return dto;
    }

    private CompanyListItem toListItem(Company company, CompanySecurity security) {
        CompanyListItem item = new CompanyListItem();
        if (security != null) {
            item.setStockCode(security.getStockCode());
            item.setStockName(security.getStockName());
            item.setMarket(security.getMarket());
            item.setListingDate(security.getListingDate());
        } else {
            item.setStockCode(company.getUnifiedCode());
            item.setStockName(company.getShortName());
        }
        item.setIndustry(company.getIndustry());
        item.setRegion(company.getRegion());
        return item;
    }

    private List<TrendDataPoint> generateFallbackTrend(String period) {
        int days = switch (period) {
            case "1m" -> 22;
            case "6m" -> 132;
            case "1y" -> 250;
            default -> 66; // 3m
        };

        List<TrendDataPoint> data = new ArrayList<>();
        LocalDate date = LocalDate.now().minusDays(days);
        double baseValue = 1000.0;

        for (int i = 0; i < days; i++) {
            TrendDataPoint point = new TrendDataPoint();
            point.setDate(date.format(DateTimeFormatter.ISO_DATE));

            double change = (Math.random() - 0.48) * 2.0;
            baseValue = baseValue * (1 + change / 100);
            point.setClose(BigDecimal.valueOf(baseValue).setScale(2, RoundingMode.HALF_UP));
            point.setChangePercent(BigDecimal.valueOf(change).setScale(2, RoundingMode.HALF_UP));

            data.add(point);
            date = date.plusDays(1);
        }
        return data;
    }
}
