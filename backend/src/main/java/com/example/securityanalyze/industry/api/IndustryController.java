package com.example.securityanalyze.industry.api;

import com.example.securityanalyze.company.api.CompanyListItem;
import com.example.securityanalyze.company.api.CompanyListResponse;
import com.example.securityanalyze.industry.application.IndustryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/industries")
@RequiredArgsConstructor
public class IndustryController {

    private final IndustryService industryService;

    @GetMapping
    public ResponseEntity<IndustryListResponse> listIndustries() {
        IndustryListResponse response = industryService.listIndustries();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{industryName}/companies")
    public ResponseEntity<CompanyListResponse> listCompaniesByIndustry(
            @PathVariable String industryName,
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

        CompanyListResponse response = industryService.listCompaniesByIndustry(industryName, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{industryName}/trend")
    public ResponseEntity<IndustryTrendResponse> getIndustryTrend(
            @PathVariable String industryName,
            @RequestParam(defaultValue = "3m") String period) {

        IndustryTrendResponse response = industryService.getIndustryTrend(industryName, period);
        return ResponseEntity.ok(response);
    }
}
