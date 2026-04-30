package com.example.securityanalyze.company.application;

import com.example.securityanalyze.company.api.CompanyDetailResponse;
import com.example.securityanalyze.company.api.CompanyListItem;
import com.example.securityanalyze.company.api.CompanyListResponse;
import com.example.securityanalyze.company.domain.Company;
import com.example.securityanalyze.company.domain.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyListResponse listCompanies(String keyword, int page, int size) {
        int offset = page * size;

        List<Company> companies = companyRepository.findByKeyword(keyword, offset, size);
        long total = companyRepository.countByKeyword(keyword);

        List<CompanyListItem> items = companies.stream()
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
        return companyRepository.findByStockCode(stockCode)
                .map(this::toDetailResponse);
    }

    private CompanyListItem toListItem(Company company) {
        CompanyListItem item = new CompanyListItem();
        item.setStockCode(company.getStockCode());
        item.setStockName(company.getStockName());
        item.setIndustry(company.getIndustry());
        item.setRegion(company.getRegion());
        item.setListingDate(company.getListingDate());
        item.setMarket(company.getMarket());
        return item;
    }

    private CompanyDetailResponse toDetailResponse(Company company) {
        CompanyDetailResponse response = new CompanyDetailResponse();
        response.setStockCode(company.getStockCode());
        response.setStockName(company.getStockName());
        response.setIndustry(company.getIndustry());
        response.setRegion(company.getRegion());
        response.setEstablishDate(company.getEstablishDate());
        response.setRegisteredCapital(company.getRegisteredCapital());
        response.setListingDate(company.getListingDate());
        response.setMarket(company.getMarket());
        return response;
    }
}
