package org.cwowhappy.securityanalyze.company.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cwowhappy.securityanalyze.company.application.dto.CompanyDTO;
import org.cwowhappy.securityanalyze.company.application.service.CompanyAppService;
import org.cwowhappy.securityanalyze.company.domain.model.Company;
import org.cwowhappy.securityanalyze.company.domain.model.CompanyId;
import org.cwowhappy.securityanalyze.company.domain.repository.CompanyRepository;
import org.cwowhappy.securityanalyze.shared.dto.PageQuery;
import org.cwowhappy.securityanalyze.shared.dto.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 公司应用服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyAppServiceImpl implements CompanyAppService {

    private final CompanyRepository companyRepository;

    @Override
    public PageResult<CompanyDTO> findByPage(PageQuery pageQuery, String industry, String province, String keyword) {
        log.debug("分页查询公司: page={}, size={}, industry={}, province={}, keyword={}",
                pageQuery.getPage(), pageQuery.getSize(), industry, province, keyword);
        PageResult<Company> result = companyRepository.findByPage(pageQuery, industry, province, keyword);
        List<CompanyDTO> dtoList = result.getList().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return PageResult.<CompanyDTO>builder()
                .list(dtoList)
                .total(result.getTotal())
                .page(result.getPage())
                .size(result.getSize())
                .build();
    }

    @Override
    public Optional<CompanyDTO> findByUscCode(String uscCode) {
        log.debug("查询公司: uscCode={}", uscCode);
        return companyRepository.findByUscCode(uscCode)
                .map(this::toDTO);
    }

    @Override
    @Transactional
    public String createCompany(CompanyDTO dto) {
        log.info("创建公司: name={}, uscCode={}", dto.getName(), dto.getUnifiedSocialCreditCode());
        Company company = Company.builder()
                .id(CompanyId.generate())
                .unifiedSocialCreditCode(dto.getUnifiedSocialCreditCode())
                .name(dto.getName())
                .shortName(dto.getShortName())
                .englishName(dto.getEnglishName())
                .formerName(dto.getFormerName())
                .legalRepresentative(dto.getLegalRepresentative())
                .chairman(dto.getChairman())
                .manager(dto.getManager())
                .secretary(dto.getSecretary())
                .regCapital(dto.getRegCapital())
                .setupDate(dto.getSetupDate())
                .province(dto.getProvince())
                .city(dto.getCity())
                .regAddress(dto.getRegAddress())
                .officeAddress(dto.getOfficeAddress())
                .website(dto.getWebsite())
                .industry(dto.getIndustry())
                .mainBusiness(dto.getMainBusiness())
                .businessScope(dto.getBusinessScope())
                .introduction(dto.getIntroduction())
                .employees(dto.getEmployees())
                .controllerName(dto.getControllerName())
                .controllerType(dto.getControllerType())
                .updatedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        CompanyId id = companyRepository.save(company);
        log.info("公司创建成功: id={}", id);
        return id.getValue();
    }

    private CompanyDTO toDTO(Company company) {
        return CompanyDTO.builder()
                .id(company.getId().getValue())
                .unifiedSocialCreditCode(company.getUnifiedSocialCreditCode())
                .name(company.getName())
                .shortName(company.getShortName())
                .englishName(company.getEnglishName())
                .formerName(company.getFormerName())
                .legalRepresentative(company.getLegalRepresentative())
                .chairman(company.getChairman())
                .manager(company.getManager())
                .secretary(company.getSecretary())
                .regCapital(company.getRegCapital())
                .setupDate(company.getSetupDate())
                .province(company.getProvince())
                .city(company.getCity())
                .regAddress(company.getRegAddress())
                .officeAddress(company.getOfficeAddress())
                .website(company.getWebsite())
                .industry(company.getIndustry())
                .mainBusiness(company.getMainBusiness())
                .businessScope(company.getBusinessScope())
                .introduction(company.getIntroduction())
                .employees(company.getEmployees())
                .controllerName(company.getControllerName())
                .controllerType(company.getControllerType())
                .updatedAt(company.getUpdatedAt())
                .createdAt(company.getCreatedAt())
                .build();
    }
}
