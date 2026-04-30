package com.example.securityanalyze.company.application;

import com.example.securityanalyze.company.api.CompanyDetailResponse;
import com.example.securityanalyze.company.api.CompanyListItem;
import com.example.securityanalyze.company.api.CompanyListResponse;
import com.example.securityanalyze.company.api.SecurityItem;
import com.example.securityanalyze.company.domain.Company;
import com.example.securityanalyze.company.domain.CompanyRepository;
import com.example.securityanalyze.company.domain.CompanySecurity;
import com.example.securityanalyze.company.domain.CompanySecurityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanySecurityRepository companySecurityRepository;

    public CompanyListResponse listCompanies(String keyword, int page, int size) {
        int offset = page * size;

        List<CompanySecurity> securities = companySecurityRepository.findByKeyword(keyword, offset, size);
        long total = companySecurityRepository.countByKeyword(keyword);

        List<CompanyListItem> items = securities.stream()
                .map(this::toListItem)
                .toList();

        CompanyListResponse response = new CompanyListResponse();
        response.setItems(items);
        response.setTotal(total);
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    public Optional<CompanyDetailResponse> getCompanyDetail(String stockCode) {
        // 1. 先查证券获取 company_id
        Optional<CompanySecurity> securityOpt = companySecurityRepository.findByStockCode(stockCode);
        if (securityOpt.isEmpty()) {
            return Optional.empty();
        }

        CompanySecurity primarySecurity = securityOpt.get();

        // 2. 查公司信息
        Optional<Company> companyOpt = companyRepository.findById(primarySecurity.getCompanyId());
        if (companyOpt.isEmpty()) {
            return Optional.empty();
        }

        Company company = companyOpt.get();

        // 3. 查该公司下的所有证券
        List<CompanySecurity> securities = companySecurityRepository.findByCompanyId(company.getId());

        return Optional.of(toDetailResponse(company, primarySecurity, securities));
    }

    private CompanyListItem toListItem(CompanySecurity security) {
        CompanyListItem item = new CompanyListItem();
        item.setStockCode(security.getStockCode());
        item.setStockName(security.getStockName());
        item.setListingDate(security.getListingDate());
        item.setMarket(security.getMarket());

        // 补充公司级信息
        companyRepository.findById(security.getCompanyId()).ifPresent(company -> {
            item.setIndustry(company.getIndustry());
            item.setRegion(company.getRegion());
        });

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
