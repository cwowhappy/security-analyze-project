package com.example.securityanalyze.company.application;

import com.example.securityanalyze.company.api.CompanyDetailResponse;
import com.example.securityanalyze.company.api.CompanyListItem;
import com.example.securityanalyze.company.api.CompanyListResponse;
import com.example.securityanalyze.company.api.SecurityItem;
import com.example.securityanalyze.company.domain.Company;
import com.example.securityanalyze.company.domain.CompanyRepository;
import com.example.securityanalyze.company.domain.CompanySecurity;
import com.example.securityanalyze.company.domain.CompanySecurityRepository;
import com.example.securityanalyze.industry.api.CompanyIndustryDto;
import com.example.securityanalyze.industry.domain.CompanyIndustryMapping;
import com.example.securityanalyze.industry.domain.CompanyIndustryMappingRepository;
import com.example.securityanalyze.industry.domain.IndustryCategory;
import com.example.securityanalyze.industry.domain.IndustryCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanySecurityRepository companySecurityRepository;
    private final CompanyIndustryMappingRepository companyIndustryMappingRepository;
    private final IndustryCategoryRepository industryCategoryRepository;

    public CompanyListResponse listCompanies(String keyword, int page, int size) {
        log.debug("查询公司列表, keyword={}, page={}, size={}", keyword, page, size);
        int offset = page * size;

        List<CompanySecurity> securities = companySecurityRepository.findByKeyword(keyword, offset, size);
        long total = companySecurityRepository.countByKeyword(keyword);

        // 批量获取公司信息，避免 N+1 查询
        List<Long> companyIds = securities.stream()
                .map(CompanySecurity::getCompanyId)
                .distinct()
                .toList();
        Map<Long, Company> companyMap = companyRepository.findAllById(companyIds).stream()
                .collect(Collectors.toMap(Company::getId, c -> c));

        List<CompanyListItem> items = securities.stream()
                .map(s -> toListItem(s, companyMap.get(s.getCompanyId())))
                .toList();

        CompanyListResponse response = new CompanyListResponse();
        response.setItems(items);
        response.setTotal(total);
        response.setPage(page);
        response.setSize(size);
        log.info("查询公司列表完成, keyword={}, 返回{}条记录", keyword, items.size());
        return response;
    }

    public Optional<CompanyDetailResponse> getCompanyDetail(String stockCode) {
        log.debug("查询公司详情, stockCode={}", stockCode);
        Optional<CompanySecurity> securityOpt = companySecurityRepository.findByStockCode(stockCode);
        if (securityOpt.isEmpty()) {
            log.warn("证券不存在, stockCode={}", stockCode);
            return Optional.empty();
        }

        CompanySecurity primarySecurity = securityOpt.get();

        Optional<Company> companyOpt = companyRepository.findById(primarySecurity.getCompanyId());
        if (companyOpt.isEmpty()) {
            log.warn("公司不存在, companyId={}", primarySecurity.getCompanyId());
            return Optional.empty();
        }

        Company company = companyOpt.get();
        List<CompanySecurity> securities = companySecurityRepository.findByCompanyId(company.getId());

        log.info("查询公司详情成功, stockCode={}, companyId={}", stockCode, company.getId());
        return Optional.of(toDetailResponse(company, primarySecurity, securities));
    }

    private CompanyListItem toListItem(CompanySecurity security, Company company) {
        CompanyListItem item = new CompanyListItem();
        item.setStockCode(security.getStockCode());
        item.setStockName(security.getStockName());
        item.setListingDate(security.getListingDate());
        item.setMarket(security.getMarket());

        if (company != null) {
            item.setIndustry(company.getIndustry());
            item.setRegion(company.getRegion());
        }

        return item;
    }

    private CompanyDetailResponse toDetailResponse(Company company, CompanySecurity primarySecurity,
                                                    List<CompanySecurity> securities) {
        CompanyDetailResponse response = new CompanyDetailResponse();
        response.setStockCode(primarySecurity.getStockCode());
        response.setStockName(primarySecurity.getStockName());
        response.setIndustry(company.getIndustry());
        response.setRegion(company.getRegion());
        response.setEstablishDate(company.getEstablishDate());
        response.setRegisteredCapital(company.getRegisteredCapital());
        response.setListingDate(primarySecurity.getListingDate());
        response.setMarket(primarySecurity.getMarket());

        List<SecurityItem> securityItems = securities.stream()
                .map(this::toSecurityItem)
                .toList();
        response.setSecurities(securityItems);

        // 填充多标准行业分类
        List<CompanyIndustryMapping> mappings = companyIndustryMappingRepository.findByCompanyId(company.getId());
        List<CompanyIndustryDto> industryDtos = new ArrayList<>();
        for (CompanyIndustryMapping mapping : mappings) {
            CompanyIndustryDto dto = new CompanyIndustryDto();
            dto.setStandardCode(mapping.getStandardCode());
            dto.setPrimary(mapping.getPrimary());

            IndustryCategory l1 = industryCategoryRepository.findByCode(mapping.getStandardCode(), mapping.getLevel1Code()).orElse(null);
            if (l1 != null) {
                dto.setLevel1Code(l1.getCode());
                dto.setLevel1Name(l1.getName());
            }
            if (mapping.getLevel2Code() != null) {
                IndustryCategory l2 = industryCategoryRepository.findByCode(mapping.getStandardCode(), mapping.getLevel2Code()).orElse(null);
                if (l2 != null) {
                    dto.setLevel2Code(l2.getCode());
                    dto.setLevel2Name(l2.getName());
                }
            }

            // 标准名称
            dto.setStandardName(switch (mapping.getStandardCode()) {
                case "SW" -> "申万行业分类";
                case "EM" -> "东方财富行业分类";
                default -> mapping.getStandardCode();
            });

            industryDtos.add(dto);
        }
        response.setIndustries(industryDtos);

        return response;
    }

    private SecurityItem toSecurityItem(CompanySecurity security) {
        SecurityItem item = new SecurityItem();
        item.setStockCode(security.getStockCode());
        item.setStockName(security.getStockName());
        item.setMarket(security.getMarket());
        item.setSecurityType(security.getSecurityType());
        item.setListingDate(security.getListingDate());
        item.setListingStatus(security.getListingStatus());
        return item;
    }
}
