package com.example.securityanalyze.company.api;

import com.example.securityanalyze.company.application.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

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

        if (size > 100) {
            size = 100;
        }
        if (size < 1) {
            size = 20;
        }
        if (page < 0) {
            page = 0;
        }

        CompanyListResponse response = companyService.listCompanies(keyword, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{stockCode}")
    public ResponseEntity<CompanyDetailResponse> getCompanyDetail(
            @PathVariable String stockCode) {

        Optional<CompanyDetailResponse> detail = companyService.getCompanyDetail(stockCode);
        return detail.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
