package org.cwowhappy.securityanalyze.company.application.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 公司应用层 DTO。
 */
@Data
@Builder
public class CompanyDTO {

    private String id;
    private String unifiedSocialCreditCode;
    private String name;
    private String shortName;
    private String englishName;
    private String formerName;
    private String legalRepresentative;
    private String chairman;
    private String manager;
    private String secretary;
    private BigDecimal regCapital;
    private LocalDate setupDate;
    private String province;
    private String city;
    private String regAddress;
    private String officeAddress;
    private String website;
    private String industry;
    private String mainBusiness;
    private String businessScope;
    private String introduction;
    private Integer employees;
    private String controllerName;
    private String controllerType;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
}
