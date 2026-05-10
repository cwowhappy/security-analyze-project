package org.cwowhappy.securityanalyze.interfaces.rest.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cwowhappy.securityanalyze.company.application.dto.CompanyDTO;
import org.cwowhappy.securityanalyze.company.application.service.CompanyAppService;
import org.cwowhappy.securityanalyze.interfaces.rest.request.CreateCompanyRequest;
import org.cwowhappy.securityanalyze.interfaces.rest.response.ApiResponse;
import org.cwowhappy.securityanalyze.shared.dto.PageQuery;
import org.cwowhappy.securityanalyze.shared.dto.PageResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 公司 REST 控制器。
 */
@Slf4j
@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyAppService companyAppService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<CompanyDTO>>> listCompanies(
            PageQuery pageQuery,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String keyword) {
        PageResult<CompanyDTO> result = companyAppService.findByPage(pageQuery, industry, province, keyword);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{uscCode}")
    public ResponseEntity<ApiResponse<CompanyDTO>> getCompany(@PathVariable String uscCode) {
        return companyAppService.findByUscCode(uscCode)
                .map(dto -> ResponseEntity.ok(ApiResponse.success(dto)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "公司不存在: " + uscCode)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<String>> createCompany(@Valid @RequestBody CreateCompanyRequest request) {
        CompanyDTO dto = CompanyDTO.builder()
                .unifiedSocialCreditCode(request.getUnifiedSocialCreditCode())
                .name(request.getName())
                .shortName(request.getShortName())
                .englishName(request.getEnglishName())
                .formerName(request.getFormerName())
                .legalRepresentative(request.getLegalRepresentative())
                .chairman(request.getChairman())
                .manager(request.getManager())
                .secretary(request.getSecretary())
                .regCapital(request.getRegCapital())
                .setupDate(request.getSetupDate())
                .province(request.getProvince())
                .city(request.getCity())
                .regAddress(request.getRegAddress())
                .officeAddress(request.getOfficeAddress())
                .website(request.getWebsite())
                .industry(request.getIndustry())
                .mainBusiness(request.getMainBusiness())
                .businessScope(request.getBusinessScope())
                .introduction(request.getIntroduction())
                .employees(request.getEmployees())
                .controllerName(request.getControllerName())
                .controllerType(request.getControllerType())
                .build();
        String id = companyAppService.createCompany(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("创建成功", id));
    }
}
